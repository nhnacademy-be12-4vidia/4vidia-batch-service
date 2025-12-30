package com.nhnacademy.book_data_batch.jobs.aladin.dto;

import com.nhnacademy.book_data_batch.jobs.aladin.dto.api.AladinItemDto;

/**
 * Aladin API 호출 및 보강 처리 결과 DTO
 * Processor에서 다음 Step으로 전달되는 데이터
 *
 * @param target 원본 BookBatchTarget DTO
 * @param itemDto 알라딘 API 응답 아이템 (성공적으로 데이터를 찾았을 경우)
 * @param isSuccess 처리 성공 여부 (데이터가 없거나, 상품 없어도 isSuccess는 true)
 * @param errorMessage 오류 메시지 (실패했을 경우)
 * @param isRetryable 실패 시 재시도 가능한 오류인지 여부
 */
public record AladinEnrichmentResult(
    BookBatchTarget target, // 변경: Batch 대신 BookBatchTarget
    AladinItemDto itemDto,
    boolean isSuccess,
    String errorMessage,
    boolean isRetryable
) {}
