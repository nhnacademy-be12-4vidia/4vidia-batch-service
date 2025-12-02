package com.nhnacademy.book_data_batch.batch.enrichment.aladin.dto;

/**
 * 보강 실패 정보
 * 
 * @param bookId  Book PK
 * @param batchId Batch PK (상태 업데이트용)
 * @param reason  실패 사유
 */
public record FailedEnrichment(
        Long bookId,
        Long batchId,
        String reason
) {}
