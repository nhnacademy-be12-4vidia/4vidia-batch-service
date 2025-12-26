package com.nhnacademy.book_data_batch.batch.core.dto;

public record BookBatchTarget(
        Long bookId,
        String isbn13,
        Long batchId
) {}
