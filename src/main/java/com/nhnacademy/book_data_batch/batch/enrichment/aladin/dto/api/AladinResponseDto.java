package com.nhnacademy.book_data_batch.batch.enrichment.aladin.dto.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * Aladin ItemLookUp API 전체 응답을 매핑하는 DTO
 * - 정상 응답: item 리스트 반환
 * - 에러 응답: errorCode, errorMessage 반환 (HTTP 200으로 옴)
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record AladinResponseDto(
        List<AladinItemDto> item,
        Integer errorCode,
        String errorMessage
) {
    /**
     * 에러 응답인지 확인
     */
    public boolean hasError() {
        return errorCode != null;
    }

    /**
     * 쿼터 초과 에러인지 확인 (errorCode: 10)
     */
    public boolean isQuotaExceeded() {
        return errorCode != null && errorCode == 10;
    }
}
