# 장애 허용 및 신뢰성 전략

대용량 배치 작업은 언제든 실패할 수 있다는 가정하에 설계되어야 합니다. 본 프로젝트에 적용된 장애 허용 전략을 설명합니다.

## 1. Defensive Configuration (방어적 설정)

Spring Batch의 `FaultTolerant` 기능을 적극 활용하여 예외 상황을 제어합니다.

### 1.1. Retry Policy (재시도 정책)
네트워크 플랩이나 일시적인 DB 락 등, **잠시 후 다시 시도하면 성공할 가능성이 있는 오류**에 적용합니다.

*   **설정 예시**:
    ```java
    .faultTolerant()
    .retryLimit(3) // Step별 상이 (Fetch: 2, Enrichment: 3)
    .retry(SocketTimeoutException.class)
    .retry(ConnectException.class)
    .retry(DeadlockLoserDataAccessException.class)
    ```
*   **적용 구간**: API 호출 Step, DB 쓰기 Step.

### 1.2. Skip Policy (건너뛰기 정책)
데이터 자체의 문제(형식 오류, 필수 값 누락)는 재시도해도 해결되지 않습니다. 이 경우 해당 아이템만 건너뛰고 나머지 작업을 계속 진행해야 합니다.

*   **설정 예시**:
    ```java
    .skipLimit(100) // 최대 100개까지만 허용 (그 이상은 데이터 품질 문제로 판단하여 Job 실패 처리)
    .skip(IllegalArgumentException.class) // 유효성 검증 실패
    .skip(JsonParseException.class) // 파싱 오류
    ```
*   **SkipLoggingListener**: 어떤 데이터가 왜 스킵되었는지 반드시 로그로 남겨 추후 보정할 수 있도록 합니다.

## 2. Transaction Management (트랜잭션 관리)

### 2.1. Chunk Size & Transaction
Spring Batch는 Chunk 단위로 트랜잭션을 커밋합니다.
*   `chunkSize=80`: 한 번에 80개의 아이템을 읽고 처리한 뒤 DB에 커밋합니다.
*   **이점**:
    *   중간에 실패하더라도 이전 Chunk까지는 커밋되어 저장됩니다(Checkpointing).
    *   트랜잭션 로그(Undo/Redo Log) 크기를 적절히 유지하여 DB 부하를 줄입니다.

### 2.2. Resuming (재시작 가능성) & Idempotency (멱등성)
배치 작업 실패 후 재시작 시, 데이터 중복 적재를 방지하고 안전하게 이어하기 위한 전략입니다.

*   **멱등성 보장 설계**:
    *   Spring Batch의 상태 저장(`saveState`) 기능과 별개로, 모든 DB 쓰기 작업은 **여러 번 실행되어도 결과가 동일하도록(`INSERT IGNORE`, `ON DUPLICATE KEY UPDATE`)** 구현했습니다.
    *   이로 인해 Job이 중간에 실패하여 재시작되더라도, 이미 처리된 데이터는 무시되거나 최신 상태로 갱신될 뿐 중복 데이터가 생성되지 않습니다.
*   **Checkpointing**:
    *   Chunk 단위로 트랜잭션이 커밋되므로, 재시작 시 실패한 Chunk의 시작 지점부터 다시 읽어들입니다.

## 3. Component Level Retry (컴포넌트 레벨 재시도)

Step 레벨(`faultTolerant`)의 재시도와는 별개로, 인프라스트럭처 계층에서 미세한 재시도 로직을 적용했습니다.

*   **JdbcExecutor**:
    *   DB 데드락이나 락 획득 실패(`CannotAcquireLockException`) 발생 시, **Step 전체를 실패 처리하지 않고** 해당 쿼리만 즉시 재시도합니다.
    *   `@Retryable(maxAttempts = 2, backoff = @Backoff(delay = 100))`
*   **API Clients**:
    *   Aladin API 등 외부 통신 시 일시적 네트워크 오류에 대해 서비스 내부적으로 재시도를 수행합니다.

## 4. Graceful Shutdown
배포 등의 이유로 애플리케이션이 종료될 때, 현재 진행 중인 Chunk 처리를 안전하게 마치고 종료되도록 설정합니다. (Spring Boot의 `lifecycle` 설정 활용)
