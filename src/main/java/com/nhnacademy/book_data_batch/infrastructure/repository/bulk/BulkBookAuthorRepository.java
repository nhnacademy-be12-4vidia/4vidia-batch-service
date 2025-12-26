package com.nhnacademy.book_data_batch.infrastructure.repository.bulk;

import com.nhnacademy.book_data_batch.batch.domain.aladin.dto.BookAuthorDto;

import java.util.List;

public interface BulkBookAuthorRepository {

    void bulkInsert(List<BookAuthorDto> bookAuthors);
}
