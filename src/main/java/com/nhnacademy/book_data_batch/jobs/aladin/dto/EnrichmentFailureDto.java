package com.nhnacademy.book_data_batch.jobs.aladin.dto;

public record EnrichmentFailureDto(
        Long batchId,
        String errorMessage
) {
}
