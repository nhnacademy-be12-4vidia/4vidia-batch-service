package com.nhnacademy.book_data_batch.repository;

import com.nhnacademy.book_data_batch.entity.Book;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookRepository extends JpaRepository<Book, Long> {

    Book findByIsbn13(String isbn13);

    boolean existsByIsbn13(String isbn13);

    List<Book> findAllByIsbn13In(Collection<String> isbns);

}
