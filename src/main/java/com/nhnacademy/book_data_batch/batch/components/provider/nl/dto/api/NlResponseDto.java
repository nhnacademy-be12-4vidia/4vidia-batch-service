package com.nhnacademy.book_data_batch.batch.components.provider.nl.dto.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record NlResponseDto(
        String totalCount,
        Integer pageNo,
        List<NlDocsDto> docs
) {
}
