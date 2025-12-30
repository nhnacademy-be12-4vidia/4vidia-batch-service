package com.nhnacademy.book_data_batch.infrastructure.repository;

import com.nhnacademy.book_data_batch.domain.BookDescriptionImage;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookDescriptionImageRepository extends JpaRepository<BookDescriptionImage, Long> {
}
