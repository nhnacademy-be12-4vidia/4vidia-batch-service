package com.nhnacademy.book_data_batch.batch.enrichment.aladin.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * Aladin API 보강 결과 DTO (순수 DTO)
 * 
 * <p>JPA 엔티티 대신 순수 DTO로 처리하여 영속성 컨텍스트 충돌 방지</p>
 * 
 * @param bookId        Book PK
 * @param batchId       Batch PK (상태 업데이트용)
 * @param description   도서 설명
 * @param priceStandard 정가
 * @param priceSales    판매가
 * @param publishedDate 출판일
 * @param subtitle      부제목
 * @param pageCount     페이지 수
 * @param bookIndex     목차
 * @param authors       저자 목록 (이름 + 역할)
 * @param tags          태그 목록 (카테고리에서 추출)
 * @param coverUrl      표지 이미지 URL
 */
public record AladinEnrichmentData(
        Long bookId,
        Long batchId,
        String description,
        Integer priceStandard,
        Integer priceSales,
        LocalDate publishedDate,
        String subtitle,
        Integer pageCount,
        String bookIndex,
        List<AuthorWithRole> authors,
        List<String> tags,
        String coverUrl
) {
    /**
     * 저자 정보 (이름 + 역할)
     * 
     * @param name 저자 이름
     * @param role 역할 (지은이, 옮긴이 등)
     */
    public record AuthorWithRole(String name, String role) {}

    /**
     * 표지 이미지 URL 존재 여부
     */
    public boolean hasCoverUrl() {
        return coverUrl != null && !coverUrl.isBlank();
    }

    /**
     * 저자 정보 존재 여부
     */
    public boolean hasAuthors() {
        return authors != null && !authors.isEmpty();
    }

    /**
     * 태그 정보 존재 여부
     */
    public boolean hasTags() {
        return tags != null && !tags.isEmpty();
    }
}
