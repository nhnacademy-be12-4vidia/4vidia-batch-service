package com.nhnacademy.book_data_batch.batch.book.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * BOOK_DB_202112.csv 한 줄을 그대로 담는 DTO
 * TODO: 이거 말고 CSV 파싱 라이브러리 쓰는 게 낫지 않나?
 * TODO: record 로 바꾸는 게 낫지 않나?
 */
@Getter
@Setter
@NoArgsConstructor
public class BookCsvRow {

    private String seqNo;
    private String isbnThirteenNo;
    private String volumeName;
    private String title;
    private String authorField;
    private String publisher;
    private String publishedDate;
    private String editionSymbol;
    private String price;
    private String imageUrl;
    private String description;
    private String kdcCode;
    private String titleSummary;
    private String authorSummary;
    private String secondaryPublishedDate;
    private String internalBookstoreYn;
    private String portalSiteYn;
    private String isbnTenNo;
}
