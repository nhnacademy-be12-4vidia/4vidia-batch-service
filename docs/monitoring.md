# Monitoring & Observability (모니터링)

운영 환경에서 비즈니스 로직을 수정하지 않고 성능 지표를 수집하기 위해 AOP를 적용했습니다.

## 1. Slow Query Detection (지연 감지)
*   **목적:** 외부 API(Aladin, Ollama) 응답이 느려져 전체 배치 성능에 영향을 주는 상황을 즉시 파악.
*   **구현:** `ApiLoggingAspect`를 통해 메소드 실행 시간이 1초(`1000ms`)를 초과할 경우 `WARN` 레벨 로그를 남김.
*   **활용:** 로그 모니터링 시스템(Prometheus/Grafana/ELK)에서 `WARN` 로그 급증 시 알림 발송.

## 2. Bulk Operation Logging (대량 처리 로깅)
*   **목적:** 수천 건의 데이터를 DB에 저장할 때의 처리 속도(Throughput) 측정.
*   **구현:** `BulkLoggingAspect`를 사용하여 `saveAll()` 등의 메소드 호출 전후로 처리 건수와 소요 시간을 로깅.
*   **효과:** 배치 성능 저하 시 병목 구간(DB vs API)을 빠르게 식별 가능.
