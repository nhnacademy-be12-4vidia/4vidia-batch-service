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

### 2.2. Resuming (재시작 가능성)
배치 작업 실패 시, `JobExecution` 상태가 `FAILED`로 남습니다. 원인을 수정(예: 네트워크 복구)한 후 동일한 파라미터로 Job을 재실행하면, **성공한 지점 이후부터 작업을 재개**합니다.
*   이를 위해 `ItemReader`가 상태(현재 읽은 위치 등)를 `ExecutionContext`에 잘 저장하도록 구현되어야 합니다.

## 3. Graceful Shutdown
배포 등의 이유로 애플리케이션이 종료될 때, 현재 진행 중인 Chunk 처리를 안전하게 마치고 종료되도록 설정합니다. (Spring Boot의 `lifecycle` 설정 활용)
