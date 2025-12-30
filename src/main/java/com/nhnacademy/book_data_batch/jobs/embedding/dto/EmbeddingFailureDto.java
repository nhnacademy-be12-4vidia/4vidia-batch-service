package com.nhnacademy.book_data_batch.jobs.embedding.dto;

public record EmbeddingFailureDto(
        Long batchId,
        String errorMessage
) {
}
