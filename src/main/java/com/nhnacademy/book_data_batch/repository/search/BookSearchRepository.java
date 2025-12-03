package com.nhnacademy.book_data_batch.repository.search;

import com.nhnacademy.book_data_batch.batch.enrichment.embedding.document.BookDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface BookSearchRepository extends ElasticsearchRepository<BookDocument, String> {

}
