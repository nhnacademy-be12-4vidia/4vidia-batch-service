# 외부 연동 심층 분석

4vidia Batch Service는 외부 시스템과의 긴밀한 연동을 통해 가치를 창출합니다. 이 과정에서 발생한 문제들과 해결 전략을 상세히 기술합니다.

## 1. Aladin Open API: 일일 호출 제한

도서 정보를 수집 및 보강을 위해 알라딘 OpenAPI에는 하나의 키 당 일일 호출 횟수 제한(5,000회)이 있습니다.

### 1.1. Multi-Key Rotation Strategy
단일 API 키로는 대규모 데이터 수집 시 한도 초과(200 OK with ErrorCode 10)에 대한 한계가 있습니다. 이를 해결하기 위해 `application.yml`에 8개의 API 키를 등록하고, `ItemReader`와 `ItemProcessor`에서 이를 순환하며 사용합니다.

```java
// AladinFetchReader.java / AladinItemProcessor.java (Logic)
private String getNextApiKey() {
    int currentIdx = keyIndex.getAndUpdate(operand -> (operand + 1) % apiKeys.size());
    return apiKeys.get(currentIdx);
}
```
*   **Effect**: 이론상 하루 40,000회(5,000 * 8)까지 호출 가능량을 확장하여 대량 수집 배치의 안정성을 확보했습니다.

### 1.2. 재시도 처리
네트워크 불안정으로 인한 일시적 실패는 Spring Retry를 통해 복구합니다.
*   **Target**: `RestClientException`, `SocketTimeoutException` 등
*   **Policy**: Step별로 상이 (신규 수집: 최대 2회, 보강: 최대 3회), 1초 간격 Backoff.

---

## 2. Ollama

온라인 서점 프로젝트에서 도서 검색의 정확도를 높이기 위해 단순 키워드 매칭을 넘어선 의미 기반 검색을 구현하고자 했습니다.
이를 위해 배치 과정에서 도서 정보를 1024차원의 벡터로 임베딩(Embedding)하여 Elasticsearch에 저장합니다.

### 2.1. Embedding Pipeline
1.  **Input**: 도서의 제목, 작가, 카테고리, 태그, 설명을 하나의 문자열로 결합.
2.  **Model**: `bge-m3` (한국어 처리에 강점이 있는 모델).
3.  **Process**:
    *   `OllamaClient`를 통해 제공받은 서버에 띄운 Ollama API로 텍스트 전송.
    *   1024차원의 벡터 데이터 수신.
4.  **Storage**: Elasticsearch의 `embedding` 필드에 저장하여 k-NN(k-Nearest Neighbors) 검색에 활용.

### 2.2. Challenges
*   **Latency**: 임베딩 생성은 외부 API(Ollama)를 호출하므로 배치 처리 속도 저하와 긴 트랜잭션의 주원인이었습니다.
*   **Solution**: 
    1.  **단계 분리 (Step Separation)**: 도서 데이터 보강(Enrichment)과 임베딩 생성을 별도의 단계(Step)로 분리했습니다. 이를 통해 도서 정보 업데이트 트랜잭션(Row Lock 보유)을 먼저 완료한 뒤, 별도의 트랜잭션에서 임베딩 작업을 수행하여 DB 락 점유 시간을 최소화했습니다.
    2.  **청크 단위 처리 (Chunk-Based Processing)**: 전체 데이터를 하나의 트랜잭션으로 처리하지 않고, 청크(Chunk) 단위로 나누어 트랜잭션을 분할 커밋했습니다. 외부 API 호출로 시간이 소요되더라도 트랜잭션이 무한정 길어지는 것을 방지하고, 시스템 리소스(DB Connection, Undo Log)를 효율적으로 관리했습니다.
    3.  **일괄 처리 최적화 (Bulk Operations)**:
        *   **RDB**: Processor에서 개별적으로 DB를 업데이트하지 않고, Writer에서 `JdbcTemplate`의 `batchUpdate`를 사용하여 처리 결과(상태값)를 한 번에 갱신함으로써 트랜잭션 빈도를 줄였습니다.
        *   **Elasticsearch**: `saveAll`을 통해 ES의 Bulk API를 활용하여 고차원 벡터 데이터를 효율적으로 인덱싱했습니다.
    4.  **조회 성능 최적화 (Fetch Strategy)**: Reader에서 `JOIN FETCH`를 사용하여 임베딩 텍스트 생성에 필요한 연관 데이터를 한 번에 조회함으로써, 처리 과정에서의 추가 쿼리 발생(N+1 문제)을 차단했습니다.

## 3. MinIO Object Storage

서비스 운영 중 발생하는 불필요한 이미지 리소스(에디터에서 업로드 되었으나 저장되지 않은 이미지)를 정리하기 위해 S3 호환 스토리지인 MinIO와 연동합니다.

### 3.1. Integration
Spring Cloud AWS 및 AWS SDK(`AmazonS3`)를 사용하여 MinIO와 통신합니다. 이를 통해 S3 프로토콜을 사용하는 모든 스토리지와 호환성을 유지합니다.

*   **주요 기능**: `ContentImageCleanupJob`을 통해 DB에서 관리되지 않는 고아 이미지(Orphan Image)를 식별하고 MinIO에서 물리적으로 삭제합니다.
