package com.nhnacademy.book_data_batch.infrastructure.repository.search;

import com.nhnacademy.book_data_batch.batch.domain.embedding.document.BookDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface BookSearchRepository extends ElasticsearchRepository<BookDocument, String> {
}
