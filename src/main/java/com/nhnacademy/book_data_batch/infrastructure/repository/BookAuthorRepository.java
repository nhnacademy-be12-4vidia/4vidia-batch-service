package com.nhnacademy.book_data_batch.infrastructure.repository;

import com.nhnacademy.book_data_batch.domain.BookAuthor;
import com.nhnacademy.book_data_batch.infrastructure.repository.bulk.BulkBookAuthorRepository;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookAuthorRepository extends JpaRepository<BookAuthor, Long>, BulkBookAuthorRepository {

    Boolean existsByAuthor_NameAndBook_Isbn(String authorName, String bookIsbn13);

}
