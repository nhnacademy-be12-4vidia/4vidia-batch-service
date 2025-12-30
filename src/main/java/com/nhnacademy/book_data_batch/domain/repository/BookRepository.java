package com.nhnacademy.book_data_batch.domain.repository;

import com.nhnacademy.book_data_batch.domain.entity.Book;
import com.nhnacademy.book_data_batch.domain.repository.custom.BookRepositoryCustom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

import java.time.LocalDate;

public interface BookRepository extends JpaRepository<Book, Long>, BookRepositoryCustom {

    List<Book> findAllByIsbnIn(Collection<String> isbns);

    @Query("SELECT b.description " +
            "FROM Book b " +
            "WHERE b.updatedAt > :updatedAt " +
            "AND b.description IS NOT NULL")
    List<String> findDescriptionsByUpdatedAtAfter(@Param("updatedAt") LocalDate updatedAt);

}
