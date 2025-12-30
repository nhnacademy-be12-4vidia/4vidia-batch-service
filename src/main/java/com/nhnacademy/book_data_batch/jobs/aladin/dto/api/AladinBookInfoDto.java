package com.nhnacademy.book_data_batch.jobs.aladin.dto.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record AladinBookInfoDto(
        String subTitle,
        Integer itemPage,
        String toc,
        List<AladinBookAuthorDto> authors
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record AladinBookAuthorDto(
            String name, // 저자 이름
            String desc  // 저자 역할
    ) {}
}
