package com.nhnacademy.book_data_batch.batch.domain.aladin.dto;

import com.nhnacademy.book_data_batch.domain.Book;

public record AladinFetchWrapper(
        Book book,
        String publisherName
) {
}
