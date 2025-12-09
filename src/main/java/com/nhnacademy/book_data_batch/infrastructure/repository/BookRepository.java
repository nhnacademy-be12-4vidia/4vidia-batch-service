package com.nhnacademy.book_data_batch.infrastructure.repository;

import com.nhnacademy.book_data_batch.domain.Book;
import com.nhnacademy.book_data_batch.infrastructure.repository.bulk.BulkBookRepository;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface BookRepository extends JpaRepository<Book, Long>, BulkBookRepository {

    List<Book> findAllByIsbnIn(Collection<String> isbns);

}
