# 성능 최적화

대량의 데이터를 제한된 시간 내에 처리하기 위해 적용한 최적화 기법들입니다.

## 1. Batch Insert Optimization (쓰기 성능 최적화)

JPA(Hibernate) 사용 시 발생하는 **N+1 Insert 문제**를 해결하기 위해 JDBC Batch 기능을 활성화했습니다.

### 1.1. Configuration
`application.yml`에 다음과 같은 설정을 추가하여 다건의 Insert 쿼리를 하나의 네트워크 패킷으로 묶어서 보냅니다.

```yaml
spring:
  datasource:
    url: jdbc:mysql://...?rewriteBatchedStatements=true # 핵심 설정
  jpa:
    properties:
      hibernate:
        order_inserts: true
        order_updates: true
        batch_size: 500 # 한 번에 묶을 쿼리 수
```

### 1.2. Result
*   단건 Insert 대비 **처리 속도 약 5배 이상 향상**.
*   DB Connection 점유 시간 단축 및 네트워크 I/O 감소.

## 2. Chunk Size Tuning (청크 사이즈 튜닝)

적절한 Chunk Size는 메모리 사용량과 처리 속도 간의 트레이드오프를 결정합니다. Step의 성격에 따라 차등 적용했습니다.

*   **설정**:
    *   **신규 수집(Fetch)**: `50` (API 응답 구조에 맞춤)
    *   **보강(Enrichment)**: `80` (`app.batch.chunk-size` 설정 값)
*   **근거**:
    *   도서 데이터 1건의 크기(이미지 URL, 상세 설명 포함)가 큼.
    *   너무 크면 OOM(Out Of Memory) 위험, 너무 작으면 트랜잭션 오버헤드 증가.
    *   벤치마크 테스트를 통해 50~100 사이가 최적임을 확인하고 설정.

## 3. Monitoring & Observability (모니터링)

성능 병목 지점을 찾고 안정적인 운영을 위해 모니터링 시스템을 구축했습니다.

### 3.1. Prometheus & Grafana
*   **Metrics**:
    *   `spring.batch.job.duration`: Job 실행 시간
    *   `spring.batch.step.duration`: Step 별 소요 시간 (어느 단계가 병목인지 파악)
    *   `spring.batch.item.read/process/write.count`: 처리 건수 추이
*   **Alerting**: Job 실패(`status=FAILED`) 또는 실행 시간이 임계치를 초과할 경우 알림 발송.

### 3.2. Actuator
`/actuator/batch` 엔드포인트를 통해 현재 실행 중인 Job의 상태를 실시간으로 확인하거나 중단할 수 있습니다.
