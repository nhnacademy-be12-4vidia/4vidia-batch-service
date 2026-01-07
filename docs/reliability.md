# Reliability & Resilience (안정성 전략)

배치 애플리케이션의 핵심인 '중단 없는 처리'와 '데이터 무결성'을 확보하기 위한 전략입니다.

## 1. Restartability (재시작 가능성)
배치 작업이 `FAILED` 상태로 종료되더라도, **처음이 아닌 실패한 지점부터** 다시 실행됩니다.

*   **구현 원리:** `JobParameters`와 `ExecutionContext`를 활용하여 현재 처리 중인 Page와 Offset 정보를 DB에 지속적으로 저장합니다.
*   **관련 코드:** `AladinFetchReader.java`, `BatchRepository`

## 2. Retry & Skip Strategy (재시도 및 건너뛰기)
외부 시스템(Aladin API, Ollama)의 불안정성에 대비합니다.

*   **Retry:** 
    *   `ResourceAccessException`(네트워크 오류) 발생 시 지수 백오프(Exponential Backoff)로 최대 3회 재시도합니다.
    *   일시적인 네트워크 튀김이나 타임아웃에 유연하게 대처합니다.
*   **Skip:** 
    *   `DataIntegrityViolationException`(데이터 포맷 오류) 발생 시 해당 항목만 건너뛰고(Skip) 나머지 배치를 계속 진행합니다.
    *   에러 로그를 남겨 추후 수동으로 보정할 수 있도록 합니다.

## 3. Quota Management (API 할당량 관리)
외부 API의 호출 제한(Rate Limit)을 준수하기 위해 트래커를 구현했습니다.
*   `AladinQuotaTracker`: 키별 사용량을 메모리 또는 Redis에 저장하여 429 Too Many Requests 에러를 사전에 방지합니다.
