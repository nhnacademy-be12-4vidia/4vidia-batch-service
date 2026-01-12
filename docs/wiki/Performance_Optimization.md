# 성능 최적화

대량의 데이터를 제한된 시간 내에 처리하기 위해 적용한 최적화 기법들입니다.

## 1. Batch Insert Optimization (쓰기 성능 최적화)

JPA(Hibernate)의 오버헤드를 피하고 성능을 극대화하기 위해 **JDBC Batch Update**를 직접 구현하여 사용했습니다.

### 1.1. Key Configuration
MySQL Driver의 `rewriteBatchedStatements=true` 옵션을 활성화하여, 수백 개의 Insert/Update 쿼리를 단 하나의 네트워크 패킷으로 재구성(Rewriting)해 전송합니다.

```yaml
spring:
  datasource:
    url: jdbc:mysql://...?rewriteBatchedStatements=true # 핵심 설정: 쿼리 패킷 병합
```

### 1.2. Custom Repository Pattern (JdbcExecutor)
Spring Batch의 기본 `JdbcBatchItemWriter` 대신 커스텀 `JdbcExecutor`를 구현하여 사용했습니다.
*   **재사용성**: Step(Chunk) 뿐만 아니라 Tasklet이나 일반 Service 로직에서도 Bulk 처리를 활용할 수 있습니다.
*   **복합 로직 처리**: `INSERT IGNORE`로 중복을 방지한 후, `SELECT ... WHERE IN (...)`으로 ID를 조회해 매핑 테이블에 적재하는 복잡한 데이터 적재 흐름을 트랜잭션 내에서 제어하기 위함입니다.

## 2. Chunk Size Tuning (청크 사이즈 튜닝)

적절한 Chunk Size는 메모리 사용량, 트랜잭션 범위, 그리고 실패 시 리스크 간의 균형을 맞추는 것이 중요합니다.

*   **설정값**: `80` (`app.batch.chunk-size`)
*   **선정 근거**:
    *   **안정성 우선**: 벤치마크상 최적의 성능 구간(500~1000)보다는 낮지만, 트랜잭션 롤백 시 재시도 비용을 줄이고 DB 커넥션 점유 시간을 짧게 가져가기 위해 보수적으로 설정했습니다.
    *   **메모리 관리**: 도서 데이터 자체는 텍스트(URL 포함) 위주라 크지 않지만, 대량 처리 시 어플리케이션 메모리 버퍼링 부하를 고려했습니다.

## 3. Monitoring & Observability (모니터링)

현재 애플리케이션은 모니터링 시스템(Prometheus)이 데이터를 수집할 수 있도록 모든 메트릭을 노출하는 준비 단계를 마쳤습니다.

### 3.1. 모니터링 준비 상태
*   **Actuator 설정 완료**: `/actuator/prometheus` 엔드포인트를 통해 Prometheus 포맷의 실시간 메트릭 노출 중.
*   **핵심 메트릭**:
    *   `spring.batch.job.duration`: Job 실행 시간 및 성공/실패 여부
    *   `spring.batch.step.duration`: Step 별 소요 시간 (병목 지점 파악 가능)
    *   `spring.batch.item.read/process/write.count`: Chunk 단위 처리 건수 및 추이

### 3.2. 모니터링 시스템 구축 가이드 (Quick Start)
인프라에 Prometheus 서버가 구축되면, 아래 설정을 통해 즉시 시각화가 가능합니다.

1.  **Prometheus 설정 (`prometheus.yml`)**:
    ```yaml
    scrape_configs:
      - job_name: '4vidia-batch-service'
        metrics_path: '/actuator/prometheus'
        static_configs:
          - targets: ['SERVER_IP:10475'] # 배치 서비스 배포 주소
    ```
2.  **Grafana 대시보드 구축**:
    *   Grafana 접속 후 Prometheus를 Data Source로 등록합니다.
    *   **Dashboard ID `11495`** (Spring Batch 전용)를 Import하면 별도의 설정 없이 고퀄리티의 배치 대시보드가 생성됩니다.

## 4. Actuator 관리 기능
`/actuator/batch` 엔드포인트를 통해 현재 실행 중인 Job의 상태를 실시간으로 확인하거나, 필요한 경우 Job을 중단할 수 있는 운영 환경을 마련했습니다.
