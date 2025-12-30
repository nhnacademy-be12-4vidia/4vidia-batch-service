package com.nhnacademy.book_data_batch.infrastructure.repository;

import com.nhnacademy.book_data_batch.domain.Book;
import com.nhnacademy.book_data_batch.infrastructure.repository.bulk.BulkBookRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

import java.time.LocalDate;

public interface BookRepository extends JpaRepository<Book, Long>, BulkBookRepository {

    List<Book> findAllByIsbnIn(Collection<String> isbns);

    @Query("SELECT b.description " +
            "FROM Book b " +
            "WHERE b.updatedAt > :updatedAt " +
            "AND b.description IS NOT NULL")
    List<String> findDescriptionsByUpdatedAtAfter(@Param("updatedAt") LocalDate updatedAt);

}
