package com.nhnacademy.book_data_batch.batch.enrichment.aladin.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * Aladin bookinfo 상세 정보 DTO
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record AladinBookInfoDto(
        String subTitle,
        Integer itemPage,
        String toc,
        List<AladinBookAuthorDto> authors
) {
    /**
     * Aladin 저자 정보 DTO
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record AladinBookAuthorDto(
            String name, // 저자 이름
            String desc  // 저자 역할
    ) {}
}
