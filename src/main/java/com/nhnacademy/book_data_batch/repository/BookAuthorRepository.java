package com.nhnacademy.book_data_batch.repository;

import com.nhnacademy.book_data_batch.entity.BookAuthor;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookAuthorRepository extends JpaRepository<BookAuthor, Long> {

    Boolean existsByAuthor_NameAndBook_Isbn13(String authorName, String bookIsbn13);

}
