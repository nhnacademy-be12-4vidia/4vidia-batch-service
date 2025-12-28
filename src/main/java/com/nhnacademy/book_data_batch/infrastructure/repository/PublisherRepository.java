package com.nhnacademy.book_data_batch.infrastructure.repository;

import com.nhnacademy.book_data_batch.domain.Publisher;

import com.nhnacademy.book_data_batch.infrastructure.repository.bulk.BulkPublisherRepository;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PublisherRepository extends JpaRepository<Publisher, Long>, BulkPublisherRepository {
    List<Publisher> findByNameIn(List<String> publisherNames);
}
