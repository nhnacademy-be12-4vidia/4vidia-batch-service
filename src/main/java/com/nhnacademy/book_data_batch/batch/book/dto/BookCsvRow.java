package com.nhnacademy.book_data_batch.batch.book.dto;

// 주어진 도서 csv의 한 행 그대로 매핑되는 DTO
public record BookCsvRow(
        String seqNo,               // 버림
        String isbn13,
        String volumeNumber,
        String title,
        String author,
        String publisher,
        String publishedDate,
        String editionSymbol,
        String price,
        String imageUrl,
        String description,
        String kdcCode,
        String titleSearch,
        String authorSearch,
        String secondaryPublishedDate,
        String internetBookstoreYn, // 버림
        String portalSiteYn,        // 버림
        String isbn10 // isbn13이 없으면 isbn10 -> isbn13 변환해 사용
) {
}
