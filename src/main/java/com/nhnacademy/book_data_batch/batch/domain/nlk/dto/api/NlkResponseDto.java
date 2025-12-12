package com.nhnacademy.book_data_batch.batch.domain.nlk.dto.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record NlkResponseDto(
        // 대문자 응답을 소문자 필드에 매핑
        String totalCount,
        String pageNo,
        List<NlkDocsDto> docs,

        // 에러 응답용 필드
        String result,
        String errCode,
        String errMessage
) {
    public boolean hasError() {
        return errCode != null && !errCode.isEmpty();
    }
}
