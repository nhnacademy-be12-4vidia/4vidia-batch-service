package com.nhnacademy.book_data_batch.domain.repository;

import com.nhnacademy.book_data_batch.domain.entity.BookDescriptionImage;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookDescriptionImageRepository extends JpaRepository<BookDescriptionImage, Long> {
}
