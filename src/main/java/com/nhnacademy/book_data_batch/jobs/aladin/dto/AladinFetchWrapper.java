package com.nhnacademy.book_data_batch.jobs.aladin.dto;

import com.nhnacademy.book_data_batch.domain.entity.Book;

public record AladinFetchWrapper(
        Book book,
        String publisherName
) {
}
