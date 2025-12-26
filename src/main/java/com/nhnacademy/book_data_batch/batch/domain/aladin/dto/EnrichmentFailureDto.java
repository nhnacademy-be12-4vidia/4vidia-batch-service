package com.nhnacademy.book_data_batch.batch.domain.aladin.dto;

public record EnrichmentFailureDto(
        Long batchId,
        String errorMessage
) {
}
