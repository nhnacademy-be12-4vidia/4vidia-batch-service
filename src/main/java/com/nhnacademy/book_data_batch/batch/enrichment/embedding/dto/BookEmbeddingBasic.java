package com.nhnacademy.book_data_batch.batch.enrichment.embedding.dto;

/**
 * 임베딩 대상 도서 기본 정보 (저자/태그 제외)
 * - QueryDSL로 조회 후 저자/태그는 별도 조회하여 조합
 */
public record BookEmbeddingBasic(
        Long bookId,
        Long batchId,
        String isbn,
        String title,
        String description,
        String publisher,
        Integer priceSales,
        Integer stock
) {
    /**
     * 저자/태그 정보를 추가하여 BookEmbeddingTarget으로 변환
     */
    public BookEmbeddingTarget toTarget(String authors, String tags) {
        return new BookEmbeddingTarget(
                bookId,
                batchId,
                isbn,
                title,
                description,
                publisher,
                priceSales,
                stock,
                authors,
                tags
        );
    }
}
