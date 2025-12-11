package com.nhnacademy.book_data_batch.batch.components.core.dto;

/**
 * Reader에서 Processor로 전달되는 보강 대상 정보
 * - Book 엔티티 대신 필요한 정보만 담은 DTO
 * - Processor에서 bookId로 실제 Book을 조회하여 사용
 *
 * @param bookId  Book PK (Processor에서 JPA 조회용)
 * @param isbn13  Aladin API 호출용
 * @param batchId Batch 상태 업데이트용
 */
public record BookBatchTarget(
        Long bookId,
        String isbn13,
        Long batchId
) {}
