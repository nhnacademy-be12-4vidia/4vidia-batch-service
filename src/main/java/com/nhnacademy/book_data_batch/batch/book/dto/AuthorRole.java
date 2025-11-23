package com.nhnacademy.book_data_batch.batch.book.dto;

import lombok.Builder;

/**
 * Parsed author information with the resolved role.
 */
@Builder
public record AuthorRole(

        // 작가 이름
        String name,

        // 작가 역할
        String role
) {
}
