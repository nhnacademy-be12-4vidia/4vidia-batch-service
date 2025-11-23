package com.nhnacademy.book_data_batch.batch.book.dto;

import java.time.LocalDate;
import java.util.List;
import lombok.Builder;

/**
 * Reader → Processor → Writer 로 전달되는 정규화된 도서 데이터입니다.
 */
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
