package com.nhnacademy.book_data_batch.infrastructure.repository;

import com.nhnacademy.book_data_batch.domain.BookImage;
import com.nhnacademy.book_data_batch.infrastructure.repository.bulk.BulkBookImageRepository;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookImageRepository extends JpaRepository<BookImage, Long>, BulkBookImageRepository {
}
