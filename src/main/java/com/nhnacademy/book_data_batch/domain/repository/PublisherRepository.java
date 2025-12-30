package com.nhnacademy.book_data_batch.domain.repository;

import com.nhnacademy.book_data_batch.domain.entity.Publisher;

import com.nhnacademy.book_data_batch.domain.repository.custom.PublisherRepositoryCustom;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PublisherRepository extends JpaRepository<Publisher, Long>, PublisherRepositoryCustom {
    List<Publisher> findByNameIn(List<String> publisherNames);
}
