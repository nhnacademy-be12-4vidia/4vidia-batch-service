package com.nhnacademy.book_data_batch.repository;

import com.nhnacademy.book_data_batch.entity.Batch;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BatchRepository extends JpaRepository<Batch, Long> {
}
