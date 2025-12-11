package com.nhnacademy.book_data_batch.batch.components.domain.search.embedding.dto;

import com.nhnacademy.book_data_batch.batch.components.domain.search.embedding.document.BookDocument;

import java.util.List;

/**
 * 임베딩 처리 대상 정보
 * - Aladin 보강이 완료된 도서 정보
 * - 임베딩 텍스트 생성 및 BookDocument 변환에 필요한 모든 정보 포함
 *
 * @param bookId      Book PK
 * @param batchId     Batch PK (상태 업데이트용)
 * @param isbn        ISBN-13
 * @param title       도서 제목
 * @param description 도서 설명
 * @param publisher   출판사명
 * @param priceSales  판매가
 * @param stock       재고
 * @param authors     저자 목록 (쉼표 구분 문자열)
 * @param tags        태그 목록 (쉼표 구분 문자열)
 */
public record BookEmbeddingTarget(
        Long bookId,
        Long batchId,
        String isbn,
        String title,
        String description,
        String publisher,
        Integer priceSales,
        Integer stock,
        String authors,
        String tags
) {
    /**
     * 임베딩 생성을 위한 텍스트 조합
     * - 제목, 설명, 저자, 태그를 조합하여 의미있는 텍스트 생성
     */
    public String buildEmbeddingText() {
        StringBuilder sb = new StringBuilder();

        sb.append("제목: ").append(title).append(" ");
        
        if (description != null && !description.isBlank()) {
            sb.append("설명: ").append(description).append(" ");
        }
        
        if (authors != null && !authors.isBlank()) {
            sb.append("저자: ").append(authors).append(" ");
        }
        
        if (tags != null && !tags.isBlank()) {
            sb.append("태그: ").append(tags).append(" ");
        }
        
        return sb.toString().trim();
    }
    
    /**
     * 임베딩 벡터를 포함한 BookDocument 생성
     */
    public BookDocument toDocument(double[] embedding) {
        return BookDocument.builder()
                .id(String.valueOf(bookId))
                .isbn(isbn)
                .title(title)
                .description(description)
                .publisher(publisher)
                .priceSales(priceSales)
                .stock(stock)
                .authors(parseCommaSeparated(authors))
                .tags(parseCommaSeparated(tags))
                .embedding(embedding)
                .build();
    }
    
    private List<String> parseCommaSeparated(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        return List.of(value.split(","));
    }
}
