package com.nhnacademy.book_data_batch.jobs.embedding.dto;

import com.nhnacademy.book_data_batch.jobs.embedding.document.BookDocument;

/**
 * 임베딩 생성 결과 DTO
 * Processor에서 Writer로 전달되는 데이터
 *
 * @param target 원본 BookEmbeddingTarget DTO
 * @param document 생성된 Elasticsearch 문서 (성공 시)
 * @param isSuccess 성공 여부
 * @param errorMessage 에러 메시지
 */
public record EmbeddingEnrichmentResult(
    BookEmbeddingTarget target, // 변경: Batch 대신 BookEmbeddingTarget
    BookDocument document,
    boolean isSuccess,
    String errorMessage
) {}
