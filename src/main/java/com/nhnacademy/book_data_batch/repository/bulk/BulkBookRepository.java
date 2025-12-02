package com.nhnacademy.book_data_batch.repository.bulk;

import com.nhnacademy.book_data_batch.batch.enrichment.aladin.dto.AladinEnrichmentData;
import com.nhnacademy.book_data_batch.entity.Book;

import java.util.List;

public interface BulkBookRepository {

    void bulkInsert(List<Book> books);

    void bulkUpdate(List<Book> books);

    /**
     * Enrichment 결과로 Book 필드 bulk 업데이트
     * 
     * @param enrichmentData 보강 결과 목록
     */
    void bulkUpdateFromEnrichment(List<AladinEnrichmentData> enrichmentData);
}
