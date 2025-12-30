package com.nhnacademy.book_data_batch.domain.repository;

import com.nhnacademy.book_data_batch.domain.entity.BookImage;
import com.nhnacademy.book_data_batch.domain.repository.custom.BookImageRepositoryCustom;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookImageRepository extends JpaRepository<BookImage, Long>, BookImageRepositoryCustom {
}
