package com.nhnacademy.book_data_batch.infrastructure.repository;

import com.nhnacademy.book_data_batch.domain.BookTag;
import com.nhnacademy.book_data_batch.infrastructure.repository.bulk.BulkBookTagRepository;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookTagRepository extends JpaRepository<BookTag, Long>, BulkBookTagRepository {
}
