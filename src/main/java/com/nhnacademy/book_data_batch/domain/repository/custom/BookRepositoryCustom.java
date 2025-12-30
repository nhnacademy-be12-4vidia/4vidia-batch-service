package com.nhnacademy.book_data_batch.domain.repository.custom;

import com.nhnacademy.book_data_batch.jobs.aladin.dto.EnrichmentSuccessDto;
import com.nhnacademy.book_data_batch.domain.entity.Book;

import java.util.List;

public interface BookRepositoryCustom {

    void bulkInsert(List<Book> books);

    void bulkUpdateFromEnrichment(List<EnrichmentSuccessDto> enrichmentData);
}
