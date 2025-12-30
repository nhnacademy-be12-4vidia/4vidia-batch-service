package com.nhnacademy.book_data_batch.domain.repository;

import com.nhnacademy.book_data_batch.domain.entity.Author;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AuthorRepository extends JpaRepository<Author, Long>, com.nhnacademy.book_data_batch.domain.repository.custom.AuthorRepositoryCustom {
}
