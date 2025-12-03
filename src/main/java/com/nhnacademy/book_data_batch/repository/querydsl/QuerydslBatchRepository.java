package com.nhnacademy.book_data_batch.repository.querydsl;

import com.nhnacademy.book_data_batch.dto.BookBatchTarget;
import com.nhnacademy.book_data_batch.dto.BookEmbeddingTarget;

import java.util.List;

public interface QuerydslBatchRepository {

    List<BookBatchTarget> findPendingEnrichmentStatusBook();
    
    List<BookEmbeddingTarget> findPendingEmbeddingStatusBook();
}
