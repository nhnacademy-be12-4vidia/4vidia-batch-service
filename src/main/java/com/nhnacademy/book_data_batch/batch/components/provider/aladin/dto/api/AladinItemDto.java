package com.nhnacademy.book_data_batch.batch.components.provider.aladin.dto.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record AladinItemDto(
        String title,
        String author,
        String pubDate,         // yyyy-MM-dd 형식
        String description,
        Integer priceStandard,
        String cover,           // 썸네일 이미지 URL
        String categoryName,    // "국내도서>건강/취미>건강정보" 형식
        String publisher,
        AladinBookInfoDto bookinfo  // 상세 정보 (ItemLookUp API에서만 제공)
) {}
