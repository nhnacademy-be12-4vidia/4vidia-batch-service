package com.nhnacademy.book_data_batch.batch.book.dto;

/**
 * 도서-작가 연관관계 DTO
 *
 * @param bookId 도서 ID (FK)
 * @param authorId 작가 ID (FK)
 * @param role 작가 역할
 */
public record BookAuthorDto(

        Long bookId,
        Long authorId,
        String role
) {

}