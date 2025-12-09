package com.nhnacademy.book_data_batch.infrastructure.repository;

import com.nhnacademy.book_data_batch.domain.Author;

import java.util.Collection;
import java.util.List;

import com.nhnacademy.book_data_batch.infrastructure.repository.bulk.BulkAuthorRepository;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuthorRepository extends JpaRepository<Author, Long>, BulkAuthorRepository {
}
