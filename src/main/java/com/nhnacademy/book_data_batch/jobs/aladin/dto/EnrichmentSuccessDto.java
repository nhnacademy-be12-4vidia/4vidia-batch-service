package com.nhnacademy.book_data_batch.jobs.aladin.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * Aladin API 보강 결과 DTO (순수 DTO)
 * 
 * @param bookId        Book PK
 * @param batchId       Batch PK (상태 업데이트용)
 * @param description   도서 설명
 * @param priceStandard 정가
 * @param publishedDate 출판일
 * @param subtitle      부제목
 * @param pageCount     페이지 수
 * @param bookIndex     목차
 * @param authors       저자 목록 (이름 + 역할)
 * @param tags          태그 목록 (카테고리에서 추출)
 * @param coverUrl      표지 이미지 URL
 * @param language      도서 언어
 */
public record EnrichmentSuccessDto(
        Long bookId,
        Long batchId,
        String description,
        Integer priceStandard,
        LocalDate publishedDate,
        String subtitle,
        Integer pageCount,
        String bookIndex,
        List<AuthorWithRole> authors,
        List<String> tags,
        String coverUrl,
        String language
) {

    /**
     * 저자 정보
     * 
     * @param name 저자 이름
     * @param role 저자 역할
     */
    public record AuthorWithRole(String name, String role) {
    }

    // cover 이미지 URL 존재 여부
    public boolean hasCoverUrl() {
        return coverUrl != null
                && !coverUrl.isBlank()
                && !coverUrl.equals("https://image.aladin.co.kr/img/noimg_b.gif");
    }

    // authors 존재 여부
    public boolean hasAuthors() {
        return authors != null && !authors.isEmpty();
    }

    // tags 존재 여부
    public boolean hasTags() {
        return tags != null && !tags.isEmpty();
    }
}
