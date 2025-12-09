package com.nhnacademy.book_data_batch.batch.enrichment.embedding.dto;

import com.nhnacademy.book_data_batch.batch.enrichment.embedding.document.BookDocument;

import java.io.Serial;
import java.io.Serializable;

/**
 * 임베딩 성공 결과
 *
 * @param bookId   Book PK
 * @param batchId  Batch PK (상태 업데이트용)
 * @param document ES 저장용 문서
 */
public record EmbeddingSuccessDto(
        Long bookId,
        Long batchId,
        BookDocument document
) implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
}
