package com.nhnacademy.book_data_batch.batch.domain.embedding.dto;

public record EmbeddingFailureDto(
        Long batchId,
        String errorMessage
) {
}
