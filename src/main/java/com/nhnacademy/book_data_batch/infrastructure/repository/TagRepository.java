package com.nhnacademy.book_data_batch.infrastructure.repository;

import com.nhnacademy.book_data_batch.domain.Tag;
import com.nhnacademy.book_data_batch.infrastructure.repository.bulk.BulkTagRepository;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public interface TagRepository extends JpaRepository<Tag, Long>, BulkTagRepository {

    List<Tag> findAllByNameIn(Collection<String> names);

    /**
     * 태그 이름 목록으로 ID 맵 조회
     * 
     * @param names 태그 이름 목록
     * @return 이름 → ID 맵
     */
    default Map<String, Long> findIdsByNames(Collection<String> names) {
        return findAllByNameIn(names).stream()
                .collect(Collectors.toMap(
                        Tag::getName,
                        Tag::getId,
                        (a, b) -> a  // 중복 시 첫 번째 값 유지
                ));
    }
}
