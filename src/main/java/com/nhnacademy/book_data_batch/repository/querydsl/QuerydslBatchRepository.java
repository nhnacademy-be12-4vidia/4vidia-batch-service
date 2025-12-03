package com.nhnacademy.book_data_batch.repository.querydsl;

import com.nhnacademy.book_data_batch.batch.enrichment.aladin.dto.BookEnrichmentTarget;
import com.nhnacademy.book_data_batch.entity.enums.BatchStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface QuerydslBatchRepository {

    List<BookEnrichmentTarget> findAllPending();
}
