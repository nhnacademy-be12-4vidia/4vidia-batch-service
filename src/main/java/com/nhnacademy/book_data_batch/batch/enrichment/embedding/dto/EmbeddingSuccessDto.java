package com.nhnacademy.book_data_batch.batch.enrichment.embedding.dto;

import com.nhnacademy.book_data_batch.batch.enrichment.embedding.document.BookDocument;

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
) {
}
