package com.nhnacademy.book_data_batch.repository;

import com.nhnacademy.book_data_batch.entity.BookAuthor;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookAuthorRepository extends JpaRepository<BookAuthor, Long> {

    Boolean existsByAuthorNameAndBookIsbn(String authorName, String isbn);

}
