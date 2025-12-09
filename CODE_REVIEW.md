# Book Data Parser - 코드 리뷰 및 평가

## 📊 종합 점수: 7.2/10

프로젝트 규모: ~6,000 LOC (110개 Java 파일)  
아키텍처: Spring Boot Batch + Spring Data JPA + Elasticsearch  
주요 기술: Virtual Threads, Concurrent Collections, Tasklet Pattern

---

## 1. 아키텍처 설계 (7.5/10)

### 강점

**1.1 관심사의 분리 (SoC)**
- ✅ **단계별 Job 분리**: BookDataJob(초기 로드) → AladinEnrichmentJob(API 보강) → 명확한 책임 분리
- ✅ **Tasklet 기반 구조**: 복잡한 비즈니스 로직을 재사용 가능한 단위로 분해
- ✅ **Mapper/Extractor 패턴**: `AladinDataMapper`, `CategoryTagExtractor` 등으로 변환 로직 독립

**1.2 병렬 처리 전략**
```java
// Virtual Threads + Partition 기반 API 병렬 호출 (AladinApiTasklet)
try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
    List<Future<?>> futures = new ArrayList<>();
    for (int i = 0; i < partitions.size(); i++) {
        futures.add(executor.submit(() -> processPartition(...)));
    }
}
```
- ✅ Virtual Threads 사용으로 경량의 동시성 (스레드풀 오버헤드 없음)
- ✅ API 키별 파티셔닝으로 쿼터 관리 최적화

### 약점

**1.3 상태 전달 메커니즘의 문제점** ⚠️
```java
// AladinApiTasklet.java
private static final ConcurrentLinkedQueue<EnrichmentSuccessDto> successResults = new ConcurrentLinkedQueue<>();

// AladinSaveTasklet.java
List<EnrichmentSuccessDto> successList = new ArrayList<>(AladinApiTasklet.getSuccessResults());
```

**문제:**
- 🔴 **Static 저장소 사용**: 메모리 누수 위험, 다중 Job 실행 시 상태 혼동
- 🔴 **Job 간 강한 결합**: Tasklet 간 직접 참조로 테스트 어려움
- 🔴 **재실행 불가능성**: 정적 상태로 인해 Step 재실행 시 오류 발생 가능

**개선안:**
```java
// StepExecutionContext 또는 ChunkContext 사용
@Override
public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {
    Map<String, Object> stepContext = chunkContext.getStepContext().getStepExecutionContext();
    List<EnrichmentSuccessDto> results = ...; // API 호출
    stepContext.put("successResults", results);
}
```

**1.4 Multi-Instance 미지원** ⚠️
- Job이 동시에 실행될 경우 정적 큐 충돌 (멀티테넌트 시스템에서 위험)
- Application.yml에 `ddl-auto: update` 설정이 각 인스턴스에서 스키마 경쟁 → 데드락 발생

---

## 2. 코드 품질 (7.0/10)

### 강점

**2.1 명확한 네이밍 컨벤션**
```java
- ALADIN_JOB_NAME, Nl_JOB_NAME (상수 명확함)
- aladinApiStep, aladinSaveStep (의도 전달)
- findPendingEnrichmentStatusBook() (메서드명 설명적)
```

**2.2 Java 21 최신 기능 활용**
- ✅ Virtual Threads (`Executors.newVirtualThreadPerTaskExecutor()`)
- ✅ Record/Sealed Classes (DTO 정의)
- ✅ Text Blocks (multi-line strings)
- ✅ Pattern Matching (향후 upgrade 가능)

**2.3 Logging**
- ✅ SLF4J + Lombok `@Slf4j` 일관성
- ✅ Progress tracking (`logInterval` 기반)
- ✅ API 키 마스킹 (보안 고려)

### 약점

**2.4 에러 처리 미흡** ⚠️

```java
// AladinApiTasklet.java - line 83-89
futures.forEach(future -> {
    try {
        future.get();
    } catch (Exception e) {
        log.error("[ALADIN] 파티션 처리 중 오류: {}", e.getMessage());
    }
});
```

**문제:**
- 🔴 **포괄적인 Exception 처리**: `Exception` 전체를 잡음 → 복구 불가능한 오류 숨김
- 🔴 **Log-only 처리**: 오류 발생 후 계속 진행 (데이터 무결성 위험)

**개선안:**
```java
futures.forEach(future -> {
    try {
        future.get();
    } catch (ExecutionException e) {
        if (e.getCause() instanceof RateLimitExceededException) {
            // 복구 가능: 대기 후 재시도
            Thread.sleep(5000);
            retryPartition(partition, apiKey);
        } else {
            // 복구 불가능: 즉시 중단
            throw new BatchProcessingException("파티션 처리 실패", e);
        }
    }
});
```

**2.5 NPE 리스크** ⚠️

```java
// AladinApiClient.java - line 103
if (response.item() == null || response.item().isEmpty()) {
    return Optional.empty();
}
return Optional.of(response.item().getFirst()); // NPE 위험: isEmpty() 체크 후 getFirst() 호출
```

**2.6 트랜잭션 경계 모호** ⚠️

```java
// EnrichmentJobConfig.java - line 96-110
@Bean
public Step aladinApiStep() {
    return new StepBuilder(ALADIN_API_STEP_NAME, jobRepository)
            .tasklet(new AladinApiTasklet(...), transactionManager)
            .build();
}
```

주석은 "트랜잭션 없음"이라고 하지만, `transactionManager` 전달 → Spring Batch가 자동으로 트랜잭션 처리
의도와 구현이 맞지 않음.

---

## 3. 동시성 및 성능 (8.0/10)

### 강점

**3.1 Virtual Threads 활용**
- ✅ I/O 대기 중 스레드 점유 안 함
- ✅ 경량 스레드로 높은 처리량 (→ 초당 수백 요청 가능)
- ✅ 스레드풀 관리 오버헤드 제거

**3.2 Quota Tracking**
```java
public boolean tryAcquire(String apiKey) {
    return usageMap.computeIfAbsent(apiKey, k -> new AtomicInteger(0))
            .getAndUpdate(current -> current < quotaPerKey ? current + 1 : current) 
            < quotaPerKey;
}
```
- ✅ Thread-safe (`ConcurrentHashMap`, `AtomicInteger`)
- ✅ Atomic 연산 (lock-free)

**3.3 Embedding 동시성 제어**
```java
// EmbeddingProcessTasklet.java
Semaphore semaphore = new Semaphore(MAX_CONCURRENT_REQUESTS);
```
- ✅ Ollama 서버 부하 제어
- ✅ Backpressure 구현

### 약점

**3.4 DB Connection 풀 부족**
```yaml
# application.yml
hikari:
  maximum-pool-size: 8
  minimum-idle: 3
```

- 🟡 Virtual Threads 100+ 동시 실행 시 DB 연결 풀 대기 (병목)
- 🟡 Aladin API 호출은 병렬이지만, 결과 저장(AladinSaveTasklet)은 단일 스레드

**개선안:**
```yaml
hikari:
  maximum-pool-size: 20  # 동시 요청에 맞춰 확대
  idle-timeout: 60000     # 유휴 연결 빠른 정리
```

**3.5 메모리 효율성** ⚠️
```java
// AladinSaveTasklet.java
List<EnrichmentSuccessDto> successList = new ArrayList<>(AladinApiTasklet.getSuccessResults());
```

- 🟡 대량 데이터(수십만 건)의 경우 메모리 폭증
- 🟡 배치 크기 제한 없음 (한 번에 모두 로드)

**개선안:**
```java
// 청크 단위 처리
List<EnrichmentSuccessDto> chunk = successList.stream()
    .skip(offset)
    .limit(BATCH_SIZE)
    .toList();
bookRepository.bulkUpdateFromEnrichment(chunk);
```

---

## 4. 데이터 무결성 & 트랜잭션 (6.5/10)

### 강점

**4.1 Bulk Operations**
- ✅ `bulkInsert()`, `bulkUpdate()` 사용 → 네트워크 왕복 최소화
- ✅ JDBC Batching 활성화 (`rewriteBatchedStatements=true`)

**4.2 Batch 상태 추적**
```java
batchRepository.bulkUpdateEnrichmentStatus(successBatchIds, BatchStatus.COMPLETED);
batchRepository.bulkUpdateEnrichmentFailed(failedData);
```
- ✅ 성공/실패 명확히 분류

### 약점

**4.3 원자성 보장 부족** ⚠️

```java
// AladinSaveTasklet.java - line 50-54
saveAuthors(successList);      // Tx1
saveTags(successList);         // Tx2
saveBooks(successList);        // Tx3
saveImages(successList);       // Tx4
updateBatchStatus(success, failed); // Tx5
```

**문제:**
- 🔴 각 메서드가 독립적인 트랜잭션
- 🔴 Author 저장은 성공, 하지만 Book 저장 실패 → 부분 커밋 (고아 데이터)

**개선안:**
```java
@Transactional
public RepeatStatus execute(...) {
    try {
        // 모든 저장 작업
        List<Author> authors = saveAuthors(successList);
        List<Tag> tags = saveTags(successList);
        // ...
        return RepeatStatus.FINISHED;
    } catch (Exception e) {
        // 전체 롤백
        throw new BatchProcessingException("Save failed", e);
    }
}
```

**4.4 Duplicate Key 처리**
```java
// AladinSaveTasklet.java - line 80
authorRepository.bulkInsert(authorNames);
Map<String, Long> authorIdMap = authorRepository.findIdsByNames(authorNames, 500);
```

- 🟡 동시 실행 시 중복 INSERT 시도 (MySQL 에러)
- 🟡 "INSERT IGNORE" 주석은 있지만 구현 확인 필요

**4.5 고아 데이터 위험**
```java
// AladinSaveTasklet - line 99-101
if (!bookAuthors.isEmpty()) {
    bookAuthorRepository.bulkInsert(bookAuthors);  // Book 저장 실패하면?
}
```

Book FK가 없으면 orphan record 생성

---

## 5. 테스트 (4.0/10)

### 현황

**5.1 기존 테스트**
- ✅ `AuthorNameExtractionTest`: CSV 파싱 테스트 (데이터 추출용)
- ✅ `AuthorParserAnalysisTest`: 작가명 파싱 전략 분석

**5.2 누락된 테스트** 🔴
- ❌ Integration Test: Batch Job 전체 흐름
- ❌ Unit Test: Tasklet 각각 (mock 필요)
- ❌ Error Scenario: API 실패, DB 오류, 네트워크 타임아웃
- ❌ Concurrency Test: 동시 실행 시 데이터 무결성

### 테스트 전략 제안

```java
// 1. Tasklet Unit Test
@Test
void testAladinApiTasklet_whenQuotaExceeded_shouldStopProcessing() {
    // Given
    AladinQuotaTracker quotaTracker = spy(new AladinQuotaTracker(10));
    doReturn(false).when(quotaTracker).tryAcquire(anyString());
    
    // When
    AladinApiTasklet tasklet = new AladinApiTasklet(batchRepo, quotaTracker, ...);
    RepeatStatus status = tasklet.execute(contribution, chunkContext);
    
    // Then
    assertEquals(RepeatStatus.FINISHED, status);
    verify(aladinApiClient, never()).lookupByIsbn(...);
}

// 2. Integration Test
@SpringBatchTest
@SpringBootTest
class AladinEnrichmentJobIntegrationTest {
    @Test
    void testAladinEnrichmentJob_endToEnd() {
        // CSV 로드 → API 호출 → DB 저장 → Elasticsearch 인덱싱
        JobExecution execution = jobLauncherTestUtils.launchJob();
        assertEquals(BatchStatus.COMPLETED, execution.getStatus());
        
        // Assertion: DB에 Author, Book, BookImage 저장됨
        List<Author> authors = authorRepository.findAll();
        assertFalse(authors.isEmpty());
    }
}

// 3. Concurrency Test
@Test
void testBulkInsert_withConcurrentRequests_shouldBeThreadSafe() {
    ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
    List<Future<?>> futures = new ArrayList<>();
    
    for (int i = 0; i < 10; i++) {
        futures.add(executor.submit(() -> {
            authorRepository.bulkInsert(Set.of("Author" + System.nanoTime()));
        }));
    }
    
    futures.forEach(f -> {
        try { f.get(); } catch (Exception e) { fail(e); }
    });
    
    List<Author> all = authorRepository.findAll();
    assertEquals(10, all.size()); // 중복 없음
}
```

---

## 6. 설정 관리 (7.0/10)

### 강점

**6.1 프로필 기반 설정**
- ✅ `application.yml` (운영)
- ✅ `application-dev.yml` (개발)
- ✅ 환경별 다른 DB/ES 설정

**6.2 외부화된 설정**
```yaml
aladin:
  api:
    keys: ttbmexlove20906001,...,ttbsungwk1000913001
    quota-per-key: 400
```
- ✅ 민감정보 환경변수화 가능

### 약점

**6.3 보안 취약점** 🔴

```yaml
datasource:
  password: Qrs8WavVsmxaQd[O  # 평문 저장!
elasticsearch:
  password: nhnacademy123!    # 평문 저장!
redis:
  password: "*N2vya7H@muDTwdNMR!"  # 평문 저장!
```

**개선안:**
```yaml
# application.yml (Git에 커밋)
datasource:
  password: ${DB_PASSWORD}

# .env 또는 K8s Secret
DB_PASSWORD=***
ES_PASSWORD=***
```

**6.4 스키마 관리 위험** ⚠️

```yaml
jpa:
  hibernate:
    ddl-auto: update  # 운영 환경에서 위험!
```

- 🔴 운영 DB에서 자동 스키마 변경 → 데이터 손실 위험
- 🔴 동시 실행 시 LOCK 경합 → 데드락

**권장사항:**
```yaml
# application.yml (운영)
ddl-auto: validate

# 수동 마이그레이션 사용 (Flyway/Liquibase)
```

**6.5 Batch Job 자동 실행 설정**

```yaml
spring:
  batch:
    job:
      enabled: true  # 애플리케이션 시작 시 모든 Job 실행!
```

- 🟡 의도하지 않은 Job 실행 위험 (테스트, 개발 환경)

**개선안:**
```yaml
spring:
  batch:
    job:
      enabled: false  # 수동 실행만

# 또는 구체적인 Job만 실행
spring:
  batch:
    job:
      names: ${BATCH_JOB_NAMES:}  # 빈 경우 실행 안 함
```

---

## 7. 모니터링 & 로깅 (7.5/10)

### 강점

**7.1 구조화된 로깅**
```java
log.info("[ALADIN] 보강 대상: {}건", pendingTargets.size());
log.info("[ALADIN] 진행률: {}% ({}/{})", percentage, currentCount, totalCount);
```
- ✅ 카테고리별 접두어 (`[ALADIN]`, `[EMBEDDING]`)
- ✅ 진행 상황 명확

**7.2 Quota Tracking**
```java
public void logUsage() {
    log.info("[AladinQuotaTracker] 사용량 현황:");
    usageMap.forEach((key, usage) -> {
        String maskedKey = key.length() > 8 ? key.substring(0, 8) + "***" : key;
        log.info("  - {}: {}/{}", maskedKey, usage.get(), quotaPerKey);
    });
}
```
- ✅ 민감정보 마스킹

### 약점

**7.3 메트릭 부재** ⚠️
- ❌ API 응답 시간 측정 없음
- ❌ 처리량(throughput) 추적 없음
- ❌ 에러율 계산 없음

**개선안 (Micrometer 활용):**
```java
@Component
public class AladinApiTasklet implements Tasklet {
    private final MeterRegistry meterRegistry;
    
    private void processPartition(...) {
        Timer.Sample sample = Timer.start(meterRegistry);
        
        try {
            Optional<AladinItemDto> response = aladinApiClient.lookupByIsbn(...);
            sample.stop(Timer.builder("aladin.api.duration")
                .tag("method", "lookupByIsbn")
                .register(meterRegistry));
        } catch (Exception e) {
            meterRegistry.counter("aladin.api.error", 
                "error_type", e.getClass().getSimpleName()).increment();
        }
    }
}
```

**7.4 Batch Job 모니터링 부족**
- 🟡 Spring Batch Admin UI 미설정
- 🟡 Job 재실행 이력 조회 불편

---

## 8. 문서화 (6.5/10)

### 강점

**8.1 클래스 레벨 주석**
```java
/**
 * Enrichment Job 설정
 * 
 * <p>Job 구성:</p>
 * <ol>
 *   <li>aladinEnrichmentStep - Aladin API로 도서 정보 보강</li>
 *   <li>embeddingStep - Ollama로 임베딩 생성 + Elasticsearch 인덱싱</li>
 *   <li>cleanupStep - 완료된 Batch 레코드 삭제</li>
 * </ol>
 */
```
- ✅ Job 흐름 명확

### 약점

**8.2 메서드 레벨 문서 부족**
- ❌ `processPartition()` 메서드 주석 없음
- ❌ 파라미터 설명 부족

**8.3 아키텍처 문서 없음**
- ❌ ERD (Entity Relationship Diagram)
- ❌ Job Flow Diagram
- ❌ API Sequence Diagram
- ❌ 인프라 구성도

**8.4 문제점 문서화** ⚠️
```java
// 테스트용: Aladin API 호출 없이 Embedding만 실행 (querydsl, document도 같이 수정 필요)
//                .start(embeddingProcessStep)
```
- 🟡 주석만 있고 해결 방안 없음

---

## 9. 의존성 관리 (7.5/10)

### 강점

**9.1 Spring Boot 3.5.7 최신 버전**
- ✅ Spring Batch 5.x (최신)
- ✅ Hibernate 6.6.33 (최신)
- ✅ Java 21 지원

**9.2 필요한 라이브러리만 포함**
- ✅ QueryDSL 5.1.0 (Jakarta)
- ✅ OpenCSV 5.9 (CSV 파싱)
- ✅ Spring Retry (재시도 로직)

### 약점

**9.3 보안 업데이트 부재**
```xml
<maven.compiler.source>21</maven.compiler.source>
```
- 🟡 Spring Security, Spring OAuth2 없음 (배치 전용이라 OK)
- 🟡 Jackson 2.17.1 (최신이 2.18.x)

**9.4 테스트 라이브러리 부족**
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-test</artifactId>
    <scope>test</scope>
</dependency>
```
- 🟡 Mockito, TestContainers 미지정 (자동 포함되지만 명시 권장)
- 🟡 AssertJ, JsonPath 미지정

---

## 10. 확장성 & 유지보수성 (6.5/10)

### 강점

**10.1 플러그인 아키텍처 가능성**
```java
// Category/Tag 추출을 전략 패턴으로 구현 가능
public interface AuthorExtractor {
    List<AuthorWithRole> extract(AladinItemDto item);
}
```

**10.2 모듈화된 구조**
```
batch/
├── book/
│   ├── processor/
│   ├── tasklet/
│   └── cache/
├── enrichment/
│   ├── aladin/
│   │   ├── client/
│   │   ├── dto/
│   │   ├── mapper/
│   │   └── tasklet/
│   └── embedding/
│       └── tasklet/
└── category/
```

### 약점

**10.3 새로운 API 추가 어려움**
- 🔴 API 클라이언트가 Aladin에만 특화
- 🔴 Quota Tracker도 Aladin 전용
- 🔴 일반화되지 않은 설계

**개선안:**
```java
// 1. API 클라이언트 인터페이스화
public interface ApiClient {
    Optional<BookEnrichedData> enrichBook(String isbn);
}

public class AladinApiClient implements ApiClient { ... }
public class NaverApiClient implements ApiClient { ... }

// 2. 동적 API 선택
@Bean
public ApiClient apiClient(@Value("${api.provider}") String provider) {
    return switch (provider) {
        case "aladin" -> new AladinApiClient(...);
        case "naver" -> new NaverApiClient(...);
        default -> throw new IllegalArgumentException("Unknown provider");
    };
}
```

**10.4 재사용성 낮음**
- 🟡 Tasklet이 구체적 구현에 의존
- 🟡 다른 프로젝트로 마이그레이션 어려움

---

## 📋 개선 우선순위

### Phase 1 (중대 결함, 즉시 수정) 🔴

1. **상태 전달 메커니즘 개선**
   - Static 큐 → StepExecutionContext
   - 영향도: 높음 | 난이도: 중상
   - 예상 일정: 2-3일

2. **트랜잭션 원자성 보장**
   - 모든 저장 작업을 하나의 @Transactional로
   - 영향도: 높음 | 난이도: 낮음
   - 예상 일정: 1일

3. **보안 취약점 수정**
   - 평문 비밀번호 → 환경변수
   - ddl-auto: validate로 변경
   - 영향도: 중상 | 난이도: 낮음
   - 예상 일정: 1일

### Phase 2 (중요 기능, 1주일 내) 🟡

4. **Integration/Unit 테스트 작성**
   - 최소 80% 커버리지 목표
   - 영향도: 중상 | 난이도: 중상
   - 예상 일정: 3-5일

5. **에러 처리 개선**
   - RateLimitExceededException 전용 처리
   - Checkpoint/Resume 로직 추가
   - 영향도: 중 | 난이도: 중
   - 예상 일정: 2-3일

6. **메모리 효율성**
   - Chunk 기반 처리로 변경
   - OOM 위험 제거
   - 영향도: 중 | 난이도: 중
   - 예상 일정: 1-2일

### Phase 3 (개선사항, 2주일 내) 🟢

7. **모니터링 & 메트릭**
   - Micrometer 통합
   - 대시보드 구성
   - 영향도: 낮음 | 난이도: 중
   - 예상 일정: 2-3일

8. **API 추상화**
   - ApiClient 인터페이스 설계
   - Naver API 연동 계획
   - 영향도: 중 | 난이도: 상
   - 예상 일정: 3-5일

9. **문서화**
   - README.md (설치, 실행 가이드)
   - ARCHITECTURE.md (설계 문서)
   - API.md (클라이언트 통합 가이드)
   - 영향도: 낮음 | 난이도: 낮음
   - 예상 일정: 2일

---

## 🎯 최종 평가

| 항목 | 점수 | 설명 |
|------|------|------|
| 아키텍처 | 7.5/10 | Virtual Threads 활용 우수, 상태 전달 메커니즘 개선 필요 |
| 코드 품질 | 7.0/10 | 명확한 네이밍, NPE/에러 처리 미흡 |
| 동시성 | 8.0/10 | Virtual Threads 및 Quota Tracking 우수, DB 풀 부족 |
| 데이터 무결성 | 6.5/10 | Bulk Operations 활용, 트랜잭션 원자성 부재 |
| 테스트 | 4.0/10 | 최소한의 테스트, 통합/단위 테스트 부재 |
| 설정 관리 | 7.0/10 | 프로필 분리 우수, 보안 취약점 있음 |
| 모니터링 | 7.5/10 | 구조화된 로깅, 메트릭 부재 |
| 문서화 | 6.5/10 | 클래스 문서 있음, 아키텍처/API 문서 부재 |
| 의존성 | 7.5/10 | 최신 라이브러리, 테스트 라이브러리 명시 필요 |
| 확장성 | 6.5/10 | 모듈화 우수, API 추상화 필요 |
| **종합** | **7.2/10** | 견고한 기초, 프로덕션 준비 70% 수준 |

### 배포 준비도: 70%

✅ **배포 가능:**
- 기본 기능 동작 (CSV 로드, API 보강, 임베딩)
- Virtual Threads로 좋은 성능
- 대부분의 에러 케이스 처리

⚠️ **배포 전 필수 수정:**
- 보안 (환경변수 비밀번호)
- 상태 전달 메커니즘
- 트랜잭션 원자성
- 기본 Integration 테스트

🟡 **배포 후 개선 권장:**
- 메트릭 추가
- API 추상화
- 고급 테스트 (chaos engineering, load test)

---

## 부록: 추천 리팩토링 코드

### 1. StepExecutionContext 기반 상태 전달

```java
@Slf4j
@RequiredArgsConstructor
public class AladinApiTasklet implements Tasklet {
    
    public static final String SUCCESS_RESULTS_KEY = "aladinSuccessResults";
    public static final String FAILED_RESULTS_KEY = "aladinFailedResults";
    
    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        Map<String, Object> stepContext = chunkContext.getStepContext().getStepExecutionContext();
        
        // 이전 데이터 초기화
        stepContext.put(SUCCESS_RESULTS_KEY, new ConcurrentLinkedQueue<>());
        stepContext.put(FAILED_RESULTS_KEY, new ConcurrentLinkedQueue<>());
        
        // ... API 호출 ...
        
        ConcurrentLinkedQueue<EnrichmentSuccessDto> successResults = 
            (ConcurrentLinkedQueue<EnrichmentSuccessDto>) stepContext.get(SUCCESS_RESULTS_KEY);
        
        log.info("[ALADIN API] 완료 - 성공: {}, 실패: {}", 
            successResults.size(), failedResults.size());
        
        return RepeatStatus.FINISHED;
    }
}

// AladinSaveTasklet
@Override
public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
    Map<String, Object> stepContext = chunkContext.getStepContext().getStepExecutionContext();
    
    @SuppressWarnings("unchecked")
    List<EnrichmentSuccessDto> successList = new ArrayList<>(
        (ConcurrentLinkedQueue<EnrichmentSuccessDto>) 
        stepContext.get(AladinApiTasklet.SUCCESS_RESULTS_KEY)
    );
    
    // ... 처리 ...
}
```

### 2. 트랜잭션 원자성

```java
@Slf4j
@RequiredArgsConstructor
public class AladinSaveTasklet implements Tasklet {
    
    private final AladinSaveService saveService;
    
    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        try {
            EnrichmentResult result = saveService.enrichAndSave(successList, failedList);
            contribution.incrementWriteCount(result.successCount());
            log.info("[ALADIN SAVE] 완료 - 성공: {}, 실패: {}", 
                result.successCount(), result.failureCount());
            return RepeatStatus.FINISHED;
        } catch (DataIntegrityViolationException e) {
            log.error("[ALADIN SAVE] 데이터 무결성 오류 - 전체 롤백됨", e);
            throw new BatchProcessingException("Save failed", e);
        }
    }
}

@Transactional  // 모든 저장을 하나의 트랜잭션으로
@Service
@Slf4j
@RequiredArgsConstructor
public class AladinSaveService {
    
    public EnrichmentResult enrichAndSave(
        List<EnrichmentSuccessDto> successList,
        List<EnrichmentFailureDto> failedList) {
        
        try {
            // 1. Author 저장
            List<Author> authors = saveAuthors(successList);
            
            // 2. Tag 저장
            List<Tag> tags = saveTags(successList);
            
            // 3. Book 저장 (FK 검증)
            List<Book> books = saveBooks(successList);
            
            // 4. BookImage 저장
            saveImages(successList);
            
            // 5. Batch 상태 업데이트
            updateBatchStatus(successList, failedList);
            
            return new EnrichmentResult(successList.size(), failedList.size());
            
        } catch (Exception e) {
            log.error("[ALADIN SAVE] 저장 중 오류, 전체 롤백: {}", e.getMessage());
            throw new RuntimeException("Batch save failed", e);  // 자동 롤백
        }
    }
    
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    private void updateBatchStatus(...) {
        // 별도 트랜잭션에서 상태 업데이트 (선택사항)
    }
}
```

### 3. 에러 처리 개선

```java
@Override
public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
    // ...
    
    try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
        List<Future<?>> futures = new ArrayList<>();
        
        for (int i = 0; i < partitions.size(); i++) {
            futures.add(executor.submit(() -> processPartition(...)));
        }
        
        // 에러 수집
        List<Exception> errors = new ArrayList<>();
        for (Future<?> future : futures) {
            try {
                future.get();
            } catch (ExecutionException e) {
                Throwable cause = e.getCause();
                
                if (cause instanceof RateLimitExceededException rateLimitEx) {
                    log.warn("[ALADIN] 쿼터 초과로 일부 데이터 처리 안 됨: {}", 
                        rateLimitEx.getMessage());
                    // 계속 진행 (부분 성공)
                } else {
                    errors.add((Exception) cause);
                }
            }
        }
        
        // 복구 불가능한 오류가 있으면 중단
        if (!errors.isEmpty()) {
            BatchProcessingException batchEx = new BatchProcessingException(
                "Partition processing failed", errors.get(0));
            errors.stream().skip(1).forEach(batchEx::addSuppressed);
            throw batchEx;
        }
    }
    
    return RepeatStatus.FINISHED;
}
```

---

**작성일**: 2024년 12월 9일  
**평가자**: Amp AI Code Reviewer
