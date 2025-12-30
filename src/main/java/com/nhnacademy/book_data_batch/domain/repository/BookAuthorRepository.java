package com.nhnacademy.book_data_batch.domain.repository;

import com.nhnacademy.book_data_batch.domain.entity.BookAuthor;
import com.nhnacademy.book_data_batch.domain.repository.custom.BookAuthorRepositoryCustom;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BookAuthorRepository extends JpaRepository<BookAuthor, Long>, BookAuthorRepositoryCustom {
}
