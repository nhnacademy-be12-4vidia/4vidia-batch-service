package com.nhnacademy.book_data_batch.domain.repository.search;

import com.nhnacademy.book_data_batch.jobs.embedding.document.BookDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface BookSearchRepository extends ElasticsearchRepository<BookDocument, String> {
}
