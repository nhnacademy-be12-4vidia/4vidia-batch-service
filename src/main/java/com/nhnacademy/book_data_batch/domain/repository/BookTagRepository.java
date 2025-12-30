package com.nhnacademy.book_data_batch.domain.repository;

import com.nhnacademy.book_data_batch.domain.entity.BookTag;
import com.nhnacademy.book_data_batch.domain.repository.custom.BookTagRepositoryCustom;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookTagRepository extends JpaRepository<BookTag, Long>, BookTagRepositoryCustom {
}
