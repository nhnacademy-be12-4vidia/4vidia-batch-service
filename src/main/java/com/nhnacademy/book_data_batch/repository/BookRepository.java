package com.nhnacademy.book_data_batch.repository;

import com.nhnacademy.book_data_batch.entity.Book;
import com.nhnacademy.book_data_batch.repository.bulk.BulkBookRepository;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface BookRepository extends JpaRepository<Book, Long>, BulkBookRepository {

    Book findByIsbn(String isbn13);

    boolean existsByIsbn(String isbn13);

    List<Book> findAllByIsbnIn(Collection<String> isbns);

}
