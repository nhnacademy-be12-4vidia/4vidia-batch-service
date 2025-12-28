package com.nhnacademy.book_data_batch.infrastructure.repository.bulk;

import java.util.Map;
import java.util.Set;

public interface BulkAuthorRepository {

    void bulkInsert(Set<String> authorNames);

    /**
     * JDBC로 저자 이름 → ID 맵 조회
     * <p>
     * Bulk INSERT 직후 JPA로 SELECT하면 MySQL 메타데이터 캐시 불일치로
     * "Table definition has changed" 오류가 발생함.
     * 따라서 INSERT와 SELECT 모두 JDBC로 처리하여 일관성 유지.
     *
     * @param names 저자 이름 목록 (중복이 제거된 집합)
     * @param batchSize IN 절 분할 크기
     * @return 이름 → ID 맵
     */
    Map<String, Long> findIdsByNames(Set<String> names, int batchSize);
}
