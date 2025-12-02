package com.nhnacademy.book_data_batch.repository.bulk;

import com.nhnacademy.book_data_batch.batch.aladin.dto.BookAuthorDto;

import java.util.List;

public interface BulkBookAuthorRepository {

    void bulkInsert(List<BookAuthorDto> bookAuthors);
}
