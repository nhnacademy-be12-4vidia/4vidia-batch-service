package com.nhnacademy.book_data_batch.batch.aladin.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * Aladin ItemLookUp API 전체 응답을 매핑하는 DTO
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record AladinResponseDto(
        List<AladinItemDto> item
) {}
