# 외부 연동 심층 분석

4vidia Batch Service는 외부 시스템과의 긴밀한 연동을 통해 가치를 창출합니다. 이 과정에서 발생한 문제들과 해결 전략을 상세히 기술합니다.

## 1. Aladin Open API: Quota & Reliability

도서 데이터의 원천인 알라딘 API는 강력하지만, **일일 호출 횟수 제한(5,000회)**과 간헐적인 응답 지연 문제가 있습니다.

### 1.1. Multi-Key Rotation Strategy
단일 API 키로는 대규모 데이터 수집 시 한도 초과(429 Too Many Requests) 오류가 발생합니다. 이를 해결하기 위해 `application.yml`에 8개의 API 키를 등록하고, `ItemReader`와 `ItemProcessor`에서 이를 순환하며 사용합니다.

```java
// AladinFetchReader.java / AladinItemProcessor.java (Logic)
private String getNextApiKey() {
    int currentIdx = keyIndex.getAndUpdate(operand -> (operand + 1) % apiKeys.size());
    return apiKeys.get(currentIdx);
}
```
*   **Effect**: 이론상 하루 40,000회(5,000 * 8)까지 호출 가능량을 확장하여 대량 수집 배치의 안정성을 확보했습니다.

### 1.2. Retry Mechanism
네트워크 불안정으로 인한 일시적 실패는 Spring Retry를 통해 복구합니다.
*   **Target**: `RestClientException`, `SocketTimeoutException` 등
*   **Policy**: Step별로 상이 (신규 수집: 최대 2회, 보강: 최대 3회), 1초 간격 Backoff.

---

## 2. Ollama & AI Embedding

검색의 정확도를 높이기 위해 단순 키워드 매칭을 넘어선 **의미 기반 검색(Semantic Search)**을 구현하고자 했습니다.

### 2.1. Embedding Pipeline
1.  **Input**: 도서의 `description`(책 소개) 및 `title`.
2.  **Model**: `bge-m3` (한국어 처리에 강점이 있는 모델).
3.  **Process**:
    *   `OllamaClient`를 통해 로컬 또는 전용 서버에 띄운 Ollama API로 텍스트 전송.
    *   1024차원의 벡터 데이터 수신.
4.  **Storage**: Elasticsearch의 `dense_vector` 필드에 저장하여 k-NN(k-Nearest Neighbors) 검색에 활용.

### 2.2. Challenges
*   **Latency**: 임베딩 생성은 CPU/GPU 연산 비용이 큽니다. 배치 처리 속도 저하의 주원인이었습니다.
*   **Solution**: 임베딩 생성 단계를 별도의 비동기 Step으로 분리하거나, 병렬 처리를 도입하여 처리량을 개선했습니다. (현재는 안정성을 위해 순차 처리 유지 중)

---

## 3. MinIO Object Storage

서비스 운영 중 발생하는 불필요한 이미지 리소스(삭제된 도서의 커버 등)를 정리하기 위해 S3 호환 스토리지인 MinIO와 연동합니다.

### 3.1. Integration
Spring Cloud AWS 및 AWS SDK(`AmazonS3`)를 사용하여 MinIO와 통신합니다. 이를 통해 S3 프로토콜을 사용하는 모든 스토리지와 호환성을 유지합니다.

*   **주요 기능**: `ContentImageCleanupJob`을 통해 DB에서 관리되지 않는 고아 이미지(Orphan Image)를 식별하고 MinIO에서 물리적으로 삭제합니다.
