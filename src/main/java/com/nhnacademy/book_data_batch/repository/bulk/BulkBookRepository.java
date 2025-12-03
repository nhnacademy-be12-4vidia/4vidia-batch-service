package com.nhnacademy.book_data_batch.repository.bulk;

import com.nhnacademy.book_data_batch.batch.enrichment.aladin.dto.EnrichmentSuccessDto;
import com.nhnacademy.book_data_batch.entity.Book;

import java.util.List;

public interface BulkBookRepository {

    void bulkInsert(List<Book> books);

    void bulkUpdateFromEnrichment(List<EnrichmentSuccessDto> enrichmentData);
}
