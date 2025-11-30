package com.nhnacademy.book_data_batch.batch.book.dto;

import java.time.LocalDate;
import java.util.List;

// 정규화된 도서 정보를 담는 DTO
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

        List<String> authorNames,
        String rawAuthorField
) {
}
