package com.nhnacademy.book_data_batch.jobs.aladin.dto;

public record BookBatchTarget(
        Long bookId,
        String isbn13,
        Long batchId
) {}
