package com.nhnacademy.book_data_batch.jobs.category_import.dto;

public record KdcCategoryCsv(

        // KDC 분류 코드
        String rawCode,

        // KDC 분류 이름
        String rawName
) {

    public boolean isEmpty() {
        return rawCode == null || rawCode.isBlank();
    }
}
