package com.nhnacademy.book_data_batch.repository;

import com.nhnacademy.book_data_batch.entity.BookTag;
import com.nhnacademy.book_data_batch.repository.bulk.BulkBookTagRepository;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookTagRepository extends JpaRepository<BookTag, Long>, BulkBookTagRepository {
}
