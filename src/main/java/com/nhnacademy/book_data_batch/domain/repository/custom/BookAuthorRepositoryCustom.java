package com.nhnacademy.book_data_batch.domain.repository.custom;

import com.nhnacademy.book_data_batch.jobs.aladin.dto.BookAuthorDto;

import java.util.List;

public interface BookAuthorRepositoryCustom {

    void bulkInsert(List<BookAuthorDto> bookAuthors);
}
