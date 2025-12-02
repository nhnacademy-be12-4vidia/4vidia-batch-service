package com.nhnacademy.book_data_batch.batch.enrichment.aladin.dto;

import com.nhnacademy.book_data_batch.entity.Book;
import com.nhnacademy.book_data_batch.entity.enums.BatchStatus;

import java.util.List;

/**
 * Processor에서 Writer로 전달되는 보강 결과 DTO
 * 
 * <p>성공 시: Book 엔티티 + 저자/태그 목록 + 이미지 URL</p>
 * <p>실패 시: bookId + 에러 메시지 (Book 조회 실패일 수 있으므로)</p>
 */
public record EnrichmentResultDto(
        Long bookId,              // 항상 존재 (성공/실패 모두)
        Book book,                // 성공 시에만 존재
        List<AuthorWithRole> authors,
        List<String> tags,
        String coverUrl,          // Aladin API에서 제공하는 표지 이미지 URL
        BatchStatus status,
        String errorMessage
) {
    /**
     * 저자 정보 (이름 + 역할)
     */
    public record AuthorWithRole(
            String name,
            String role  // "지은이", "옮긴이" 등
    ) {}

    /**
     * 보강 성공
     */
    public static EnrichmentResultDto success(Book book, List<AuthorWithRole> authors, List<String> tags, String coverUrl) {
        return new EnrichmentResultDto(
                book.getId(),
                book,
                authors,
                tags,
                coverUrl,
                BatchStatus.COMPLETED,
                null
        );
    }

    /**
     * 보강 실패 (Book 조회 실패 포함)
     */
    public static EnrichmentResultDto failure(Long bookId, String errorMessage) {
        return new EnrichmentResultDto(
                bookId,
                null,
                List.of(),
                List.of(),
                null,
                BatchStatus.FAILED,
                errorMessage
        );
    }

    /**
     * 성공 여부 확인
     */
    public boolean isSuccess() {
        return status == BatchStatus.COMPLETED;
    }
    
    /**
     * 이미지 URL 존재 여부
     */
    public boolean hasCoverUrl() {
        return coverUrl != null && !coverUrl.isBlank();
    }
}
