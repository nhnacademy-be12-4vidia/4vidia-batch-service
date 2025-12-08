package com.nhnacademy.book_data_batch.infrastructure.repository;

import com.nhnacademy.book_data_batch.domain.Author;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.HashMap;

import com.nhnacademy.book_data_batch.infrastructure.repository.bulk.BulkAuthorRepository;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuthorRepository extends JpaRepository<Author, Long>, BulkAuthorRepository {

    boolean existsByName(String name);

    Author findByName(String name);

    List<Author> findAllByNameIn(Collection<String> names);

    /**
     * 저자 이름 목록으로 ID 맵 조회
     * 
     * @param names 저자 이름 목록
     * @return 이름 → ID 맵
     */
    default Map<String, Long> findIdsByNames(Collection<String> names) {
        return findIdsByNames(names, 500);
    }

    /**
     * 저자 이름 목록으로 ID 맵 조회 (배치로 분할 조회)
     * - MySQL에서 매우 큰 IN 절 사용 시 간헐적 "Table definition has changed" 오류를 회피하기 위해 분할 조회
     *
     * @param names     저자 이름 목록
     * @param batchSize 분할 크기
     * @return 이름 → ID 맵
     */
    default Map<String, Long> findIdsByNames(Collection<String> names, int batchSize) {
        Map<String, Long> result = new HashMap<>();
        if (names == null || names.isEmpty()) {
            return result;
        }

        List<String> list = (names instanceof List)
                ? (List<String>) names
                : new ArrayList<>(names);

        for (int i = 0; i < list.size(); i += batchSize) {
            int end = Math.min(i + batchSize, list.size());
            List<String> chunk = list.subList(i, end);
            findAllByNameIn(chunk).forEach(author ->
                    result.putIfAbsent(author.getName(), author.getId())
            );
        }

        return result;
    }
}
