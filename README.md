# 4vidia Batch Service

4vidia 온라인 서점의 도서 데이터 적재, 할인 정책 반영, 리소스 정리를 수행하는 배치 애플리케이션입니다.

## ⚙️ 주요 작업 (Batch Jobs)

### 1. 도서 데이터 관리
*   **[KDC 카테고리 적재](docs/wiki/batch_jobs/KdcCategoryJob.md)**: 한국십진분류법(KDC)을 활용한 계층형 카테고리를 구축합니다.
*   **[도서 초기 데이터 적재](docs/wiki/batch_jobs/BookDataImportJob.md)**: CSV 기반의 대량 도서 데이터를 DB에 적재합니다.
*   **[도서 정보 보강 및 임베딩](docs/wiki/batch_jobs/Aladin_Book_Integration_Jobs.md)**: 알라딘 API를 통한 데이터 보강 및 Ollama를 이용한 도서 정보(제목, 설명, 저자 등) 임베딩 벡터를 생성하여 Elasticsearch에 반영합니다.

### 2. 가격 정책 반영
*   **[도서 할인가 재계산](docs/wiki/batch_jobs/DiscountRepriceJob.md)**: 할인 정책 변경 시 대상 도서의 할인가를 재계산하여 MySQL과 Elasticsearch에 업데이트합니다.

### 3. 리소스 정리
*   **[이미지 리소스 정리](docs/wiki/batch_jobs/ContentImageCleanupJob.md)**: 도서 설명에 사용되는 에디터에서 업로드 되었으나 삭제되지 않은 이미지를 MinIO 스토리지에서 정리합니다.

## 📑 상세 설계 및 전략 (Detailed Strategy)

배치 서비스 운영과 성능 향상을 위한 기술적 구현 상세입니다.

*   **[시스템 아키텍처 및 기술적 의사결정](docs/wiki/System_Architecture.md)**: Spring Batch 도입 이유와 핵심 패턴(Chunk, Event-Driven) 설명
*   **[성능 최적화 전략](docs/wiki/Performance_Optimization.md)**: JDBC Batch Update 및 청크 사이즈 튜닝, 모니터링 가이드
*   **[장애 허용 및 신뢰성 전략](docs/wiki/Fault_Tolerance_and_Reliability.md)**: 재시도(Retry), 건너뛰기(Skip) 정책 및 트랜잭션 관리
*   **[외부 시스템 연동 상세](docs/wiki/External_Integrations.md)**: 알라딘 API 키 로테이션 및 Ollama 임베딩 파이프라인 상세

## 🛠 기술 스택

*   **Framework**: Spring Batch 5, Spring Boot 3.x
*   **Database**: MySQL, Elasticsearch
*   **Storage**: MinIO
*   **Messaging**: RabbitMQ (배치 트리거용)
*   **External API**: Aladin API, Ollama

## 🚀 실행 방법

### 빌드
```bash
./mvnw clean package
```

### 특정 Job 수동 실행
```bash
# 예: 도서 데이터 적재 실행
java -jar target/4vidia-batch-service-0.0.1-SNAPSHOT.jar --job.name=bookDataImportJob
```
