package com.nhnacademy.book_data_batch.batch.components.provider.aladin.dto;

import java.io.Serial;
import java.io.Serializable;

/**
 * 보강 실패 정보
 * 
 * @param bookId  Book PK
 * @param batchId Batch PK (상태 업데이트용)
 * @param reason  실패 사유
 */
public record EnrichmentFailureDto(
        Long bookId,
        Long batchId,
        String reason
) implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
}
