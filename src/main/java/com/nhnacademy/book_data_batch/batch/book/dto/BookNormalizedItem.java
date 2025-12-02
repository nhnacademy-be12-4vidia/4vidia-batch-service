package com.nhnacademy.book_data_batch.batch.book.dto;

import java.time.LocalDate;

/**
 * 정규화된 도서 정보를 담는 DTO
 * - CSV에서 읽은 원시 데이터를 정규화한 결과
 * - 작가 정보는 rawAuthor(원본)만 저장 (정규화된 작가는 알라딘 API에서 처리)
 */
public record BookNormalizedItem(
        String isbn13,
        String title,
        String description,
        LocalDate publishedDate,
        Integer priceStandard,
        Integer volumeNumber,
        String imageUrl,
        String kdcCode,
        String publisherName,
        String rawAuthor  // CSV 원본 작가 필드 (검색/임베딩용)
) {
}
