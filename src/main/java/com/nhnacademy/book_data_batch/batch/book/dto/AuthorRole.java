package com.nhnacademy.book_data_batch.batch.book.dto;

import lombok.Builder;

// 작가 이름과 역할을 담는 DTO
@Builder
public record AuthorRole(

        // 작가 이름
        String name,

        // 작가 역할
        String role
) {
}
