package com.nhnacademy.book_data_batch.infrastructure.repository.querydsl;

import com.nhnacademy.book_data_batch.batch.dto.BookBatchTarget;
import com.nhnacademy.book_data_batch.batch.enrichment.embedding.dto.BookEmbeddingTarget;

import java.util.List;

public interface QuerydslBatchRepository {

    List<BookBatchTarget> findPendingEnrichmentStatusBook();
    
    List<BookEmbeddingTarget> findPendingEmbeddingStatusBook();
}
