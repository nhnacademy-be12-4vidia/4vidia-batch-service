package com.nhnacademy.book_data_batch.infrastructure.repository;

import com.nhnacademy.book_data_batch.domain.BookTag;
import com.nhnacademy.book_data_batch.infrastructure.repository.bulk.BulkBookTagRepository;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BookTagRepository extends JpaRepository<BookTag, Long>, BulkBookTagRepository {
    List<BookTag> findByBookId(Long bookId);
}
