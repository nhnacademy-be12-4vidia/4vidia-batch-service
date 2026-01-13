# 시스템 아키텍처 및 기술적 의사결정

## 1. System Overview

**4vidia Batch Service**는 주어진 15만건의 도서 데이터를 안정적으로 적재하고, 시스템 간 데이터 정합성을 유지하기 위한 **Spring Batch 기반의 백엔드 애플리케이션**입니다.

단순한 작업 스케줄러(Cron)를 넘어, 외부 API로부터 대규모 데이터를 수집(Import), 가공(Enrich)하고, 비즈니스 로직(할인 정책 등)을 적용하여 **MySQL과 Elasticsearch 간의 데이터 일관성을 보장**하는 역할을 수행합니다.

## 2. Key Architecture Patterns (핵심 아키텍처 패턴)

### 2.1. Chunk-Oriented Processing (청크 지향 처리)
수십만 건의 데이터를 메모리에 모두 로드하지 않고, 설정된 크기(`Chunk Size`)만큼 끊어서 읽고(Read) 처리(Process)한 뒤 트랜잭션을 커밋(Write)합니다.
*   **구현**: `Fetched (50)` / `Enriched (80)` 등 작업 성격에 따라 최적화된 청크 사이즈를 적용하여 OOM(Out Of Memory) 방지 및 처리 속도를 조절합니다.

### 2.2. Event-Driven Batch (이벤트 구동 배치)
정해진 시간(Cron)에만 실행되는 전통적인 배치의 한계를 극복하기 위해, 비즈니스 이벤트 발생 시 즉시 배치를 실행하는 **하이브리드 트리거** 방식을 채택했습니다.
*   **구현**: RabbitMQ의 `DiscountPolicyChangedEvent`를 리스닝하는 Consumer가 `DiscountRepriceJob`을 즉시 실행하여, 정책 변경 사항을 실시간에 가깝게 반영합니다.

## 3. Technical Decisions (기술적 의사결정 - ADR)

### 3.1. Why Spring Batch?
단순 루프나 쉘 스크립트가 아닌 Spring Batch 프레임워크를 도입한 이유는 다음과 같습니다.
*   **Memory Efficiency (Chunk Processing)**: 대용량 데이터를 한 번에 로드하지 않고 끊어서 처리함으로써, OOM 없이 안정적인 처리가 가능합니다.
*   **Restartability**: `Batch_Job_Execution` 메타 테이블을 통해 실패 지점을 기록하고, 문제 해결 후 **중단된 지점부터 이어서 실행**할 수 있습니다.
*   **Standardized Flow**: `Reader` -> `Processor` -> `Writer`의 정형화된 패턴을 통해 코드의 유지보수성과 가독성을 높였습니다.

### 3.2. Why JDBC Batch Updates?
ORM(JPA)의 `saveAll` 사용 시 발생하는 성능 저하(N+1 Insert)를 해결하기 위함입니다.
*   **설정**: `rewriteBatchedStatements=true` 옵션과 `JdbcTemplate Batch Update`를 사용하여, 수천 건의 Insert/Update 쿼리를 단일 네트워크 패킷으로 묶어 전송함으로써 쓰기 성능을 극대화했습니다.

### 3.3. Why Dual Storage (MySQL + Elasticsearch)?
*   **책임 분리**:
    *   **MySQL**: 관계형 데이터의 원본 저장 및 트랜잭션 처리를 담당합니다.
    *   **Elasticsearch**: 형태소 분석, 복합 필터링 등 고성능 검색 기능을 담당합니다.
*   **Consistency Responsibility**: Batch Service는 비즈니스 로직이 적용된 최종 데이터를 이 두 저장소에 동시에 반영하여, **검색 결과와 상세 조회 결과의 불일치를 방지**합니다.

### 3.4. External API Strategy (Rate Limiting)
알라딘 API의 엄격한 호출 제한(Rate Limit)을 우회하기 위해 **Multi-Key Rotation** 전략을 사용합니다.
*   8개의 API 키를 리스트로 관리하며, 요청 시마다 키를 순환(`Round Robin`)시켜 단일 키의 쿼터 소진을 방지하고 수집 가용성을 확보했습니다.

### 3.5. Why RabbitMQ?
배치 실행을 위해 단순 API 호출이 아닌 메시지 큐(RabbitMQ)를 도입한 이유는 다음과 같습니다.
*   **Decoupling**: 도서 서비스나 쿠폰 서비스 등 업스트림 시스템이 배치 서비스의 실행 상태나 지연에 영향을 받지 않도록 결합도를 낮췄습니다.
*   **Asynchronous Processing**: 할인 정책 변경과 같이 시간이 오래 걸리는 재계산 작업(Repricing)을 비동기로 처리하여, 사용자 요청의 응답 속도를 저하시키지 않습니다.
*   **Reliability**: 일시적인 배치 서비스 장애 시에도 메시지가 큐에 보존(Durability)되므로, 작업 요청이 유실되지 않고 복구 후 처리가 가능합니다.

