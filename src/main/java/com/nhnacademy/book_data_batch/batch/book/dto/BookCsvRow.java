package com.nhnacademy.book_data_batch.batch.book.dto;

import lombok.Builder;

// 주어진 도서 csv의 한 행 그대로 매핑되는 DTO
@Builder
public record BookCsvRow(
    String seqNo,
    String isbnThirteenNo,
    String volumeName,
    String title,
    String authorField,
    String publisher,
    String publishedDate,
    String editionSymbol,
    String price,
    String imageUrl,
    String description,
    String kdcCode,
    String titleSummary,
    String authorSummary,
    String secondaryPublishedDate,
    String internalBookstoreYn,
    String portalSiteYn,
    String isbnTenNo
) {
}
