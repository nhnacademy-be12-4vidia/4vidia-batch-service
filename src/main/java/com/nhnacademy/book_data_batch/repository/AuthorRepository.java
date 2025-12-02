package com.nhnacademy.book_data_batch.repository;

import com.nhnacademy.book_data_batch.entity.Author;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.nhnacademy.book_data_batch.repository.bulk.BulkAuthorRepository;
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
        return findAllByNameIn(names).stream()
                .collect(java.util.stream.Collectors.toMap(
                        Author::getName,
                        Author::getId,
                        (a, b) -> a  // 중복 시 첫 번째 값 유지
                ));
    }
}
