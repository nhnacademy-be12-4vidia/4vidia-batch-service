# 4vidia Batch Service

온라인 서점 팀 프로젝트에서 데이터 처리 및 스케줄링 작업을 담당하는 배치 서비스입니다.

## 🛠 기술 스택 (Tech Stack)

### Language & Framework
![Java](https://img.shields.io/badge/Java-21-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.5.7-6DB33F?style=for-the-badge&logo=spring-boot&logoColor=white)
![Spring Batch](https://img.shields.io/badge/Spring_Batch-6DB33F?style=for-the-badge&logo=spring&logoColor=white)

### Database & Storage
![MySQL](https://img.shields.io/badge/MySQL-4479A1?style=for-the-badge&logo=mysql&logoColor=white)
![Elasticsearch](https://img.shields.io/badge/Elasticsearch-005571?style=for-the-badge&logo=elasticsearch&logoColor=white)
![MinIO](https://img.shields.io/badge/MinIO-C72E49?style=for-the-badge&logo=minio&logoColor=white)

### Messaging
![RabbitMQ](https://img.shields.io/badge/RabbitMQ-FF6600?style=for-the-badge&logo=rabbitmq&logoColor=white)

### Monitoring & QA
![Prometheus](https://img.shields.io/badge/Prometheus-E6522C?style=for-the-badge&logo=prometheus&logoColor=white)
![Spring Actuator](https://img.shields.io/badge/Spring_Actuator-6DB33F?style=for-the-badge&logo=spring&logoColor=white)
![SonarQube](https://img.shields.io/badge/SonarQube-4E9BCD?style=for-the-badge&logo=sonarqube&logoColor=white)
![JaCoCo](https://img.shields.io/badge/JaCoCo-Coverage-green?style=for-the-badge)

**Key Libraries & Utilities**
*   **Resilience:** `Spring Retry` (외부 API 호출 및 DB 작업 실패 시 자동 재시도)
*   **Data Processing:** `OpenCSV` (대용량 도서 데이터 CSV 파싱)
*   **Optimization:** `Spring JDBC` (Batch Insert 성능 최적화를 위한 직접 사용)

## 📚 상세 문서

*   **[01. 시스템 아키텍처](docs/wiki/System_Architecture.md)**: 전체 시스템 구성도 및 기술적 의사결정 (Why Spring Batch & RabbitMQ?)
*   **[02. 외부 시스템 연동 전략](docs/wiki/External_Integrations.md)**: Aladin API 쿼터 제한 극복, AI 임베딩 파이프라인, MinIO 연동 전략
*   **[03. 장애 허용 및 안정성 확보](docs/wiki/Fault_Tolerance_and_Reliability.md)**: 장애 허용을 위한 Retry/Skip 정책 및 트랜잭션 관리
*   **[04. 성능 최적화](docs/wiki/Performance_Optimization.md)**: 대량 Insert 최적화 및 Chunk Size 튜닝 경험

## 📅 주요 배치 작업 명세 (Batch Jobs Specification)

### 운영 배치 (Operational Jobs)
*   **[도서 가격 재계산 (DiscountRepriceJob)](docs/wiki/batch_jobs/DiscountRepriceJob.md)**: (매일 00:00) 할인 정책 변경에 따른 도서 가격 일괄 재계산.
*   **[이미지 리소스 정리 (ContentImageCleanupJob)](docs/wiki/batch_jobs/ContentImageCleanupJob.md)**: (매일 03:00) 미사용 이미지 리소스 정리.

### 초기화 및 유틸리티 (Initialization & Utility)
*   **[대용량 도서 초기 적재 (BookDataImportJob)](docs/wiki/batch_jobs/BookDataImportJob.md)**: 대용량 CSV 파일 기반 도서 데이터 초기 적재.
*   **[KDC 카테고리 적재 (KdcCategoryJob)](docs/wiki/batch_jobs/KdcCategoryJob.md)**: 카테고리 계층 구조 초기 적재.
*   **[데이터 보강 재처리 (AladinEnrichmentJob)](docs/wiki/batch_jobs/Aladin_Book_Integration_Jobs.md)**: 기존 데이터의 보강 및 임베딩 재생성.
*   **[알라딘 신간 수집 및 임베딩 (AladinNewBookImportJob)](docs/wiki/batch_jobs/Aladin_Book_Integration_Jobs.md)**: 신규 도서 데이터 수집 및 AI 임베딩 생성.


## ⚙️ 설정 및 구성 (Configuration)

주요 설정은 `src/main/resources/application.yml`에서 관리됩니다.

### 배치 설정
```yaml
spring:
  batch:
    job:
      enabled: false  # 애플리케이션 시작 시 자동 실행 방지
    jdbc:
      initialize-schema: always # 배치 메타 데이터 테이블 자동 생성
```

## 🚀 실행 방법 (How to Run)

### 빌드 (Build)
```bash
./mvnw clean package
```

### 실행 (Run)
```bash
java -jar target/4vidia-batch-service-0.0.1-SNAPSHOT.jar
```
또는 Maven Wrapper를 사용하여 직접 실행:
```bash
./mvnw spring-boot:run
```

---
© 2026 4VIDIA Team.