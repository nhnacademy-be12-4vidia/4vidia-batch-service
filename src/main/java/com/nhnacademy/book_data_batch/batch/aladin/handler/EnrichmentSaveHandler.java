package com.nhnacademy.book_data_batch.batch.aladin.handler;

import com.nhnacademy.book_data_batch.batch.aladin.dto.EnrichmentResultDto;

import java.util.List;

/**
 * 보강 결과 저장을 처리하는 인터페이스
 */
public interface EnrichmentSaveHandler {

    /**
     * 보강 결과 저장 처리
     *
     * @param items 보강 결과 목록
     */
    void handle(List<EnrichmentResultDto> items);

    /**
     * Handler 실행 순서 (낮을수록 먼저 실행)
     */
    default int getOrder() {
        return 0;
    }
}
