package com.nhnacademy.book_data_batch.batch.components.provider.nl.dto.api;

public record NlDocsDto (
        String publisher,
        String updateDate,
        String bookTbCnt,
        String bookSummary,
        String author,
        String kdc,
        String realPublishDate,
        String titleUrl,
        Integer prePrice,
        String page,
        String eaIsbn,
        String inputDate,
        String bookIntroduction,
        String vol,
        String title,
        String publish_predate
){
}
