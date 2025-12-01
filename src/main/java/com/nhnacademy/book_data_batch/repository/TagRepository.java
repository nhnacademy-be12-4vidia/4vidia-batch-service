package com.nhnacademy.book_data_batch.repository;

import com.nhnacademy.book_data_batch.entity.Tag;
import com.nhnacademy.book_data_batch.repository.bulk.BulkTagRepository;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface TagRepository extends JpaRepository<Tag, Long>, BulkTagRepository {

    List<Tag> findAllByNameIn(Collection<String> names);
}
