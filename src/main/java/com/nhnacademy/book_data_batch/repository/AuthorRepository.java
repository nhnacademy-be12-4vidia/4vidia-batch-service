package com.nhnacademy.book_data_batch.repository;

import com.nhnacademy.book_data_batch.entity.Author;

import java.util.Collection;
import java.util.List;

import com.nhnacademy.book_data_batch.repository.bulk.BulkAuthorRepository;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuthorRepository extends JpaRepository<Author, Long>, BulkAuthorRepository {

    boolean existsByName(String name);

    Author findByName(String name);

    List<Author> findAllByNameIn(Collection<String> names);
}
