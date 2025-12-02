package com.nhnacademy.book_data_batch.batch.enrichment.aladin.dto;

/**
 * 도서-작가 연관관계 DTO
 * - 알라딘 API에서 받은 작가 정보를 BookAuthor 테이블에 저장할 때 사용
 *
 * @param bookId 도서 ID (FK)
 * @param authorId 작가 ID (FK)
 * @param role 작가 역할 (알라딘 API의 desc 필드: "지은이", "옮긴이" 등)
 */
public record BookAuthorDto(
        Long bookId,
        Long authorId,
        String role
) {
}
