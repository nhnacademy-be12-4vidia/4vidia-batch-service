package com.nhnacademy.book_data_batch.repository;

import com.nhnacademy.book_data_batch.entity.BookImage;
import com.nhnacademy.book_data_batch.repository.bulk.BulkBookImageRepository;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookImageRepository extends JpaRepository<BookImage, Long>, BulkBookImageRepository {
}
