package com.nhnacademy.book_data_batch.infrastructure.repository;

import com.nhnacademy.book_data_batch.domain.Publisher;

import java.util.Collection;
import java.util.List;

import com.nhnacademy.book_data_batch.infrastructure.repository.bulk.BulkPublisherRepository;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PublisherRepository extends JpaRepository<Publisher, Long>, BulkPublisherRepository {
}
