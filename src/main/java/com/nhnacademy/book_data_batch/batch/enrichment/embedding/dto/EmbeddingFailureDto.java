package com.nhnacademy.book_data_batch.batch.enrichment.embedding.dto;

import java.io.Serial;
import java.io.Serializable;

/**
 * 임베딩 실패 결과
 *
 * @param bookId  Book PK
 * @param batchId Batch PK (상태 업데이트용)
 * @param reason  실패 사유
 */
public record EmbeddingFailureDto(
        Long bookId,
        Long batchId,
        String reason
) implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
}
