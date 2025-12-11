package com.nhnacademy.book_data_batch.infrastructure.repository.bulk;

import com.nhnacademy.book_data_batch.batch.components.provider.aladin.dto.BookAuthorDto;

import java.util.List;

public interface BulkBookAuthorRepository {

    void bulkInsert(List<BookAuthorDto> bookAuthors);
}
