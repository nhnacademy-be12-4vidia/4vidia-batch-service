package com.nhnacademy.book_data_batch.batch.book.dto;

import java.time.LocalDate;
import java.util.List;
import lombok.Builder;

// 정규화된 도서 정보를 담는 DTO
@Builder
public record BookNormalizedItem(
        String isbn13,
        String title,
        String subtitle,
        List<AuthorRole> authorRoles,
        String publisherName,
        Integer priceStandard,
        String imageUrl,
        String description,
        LocalDate publishedDate,
        String kdcCode,
        Integer volumeNumber
) {
}
