package com.nhnacademy.book_data_batch.batch.enrichment.aladin.extractor;

import com.nhnacademy.book_data_batch.batch.enrichment.aladin.dto.AladinEnrichmentData.AuthorWithRole;
import com.nhnacademy.book_data_batch.batch.enrichment.aladin.dto.AladinItemDto;

import java.util.List;

/**
 * Aladin API 응답에서 저자 정보를 추출하는 인터페이스
 */
public interface AuthorExtractor {

    /**
     * Aladin API 응답에서 저자 목록 추출
     * 
     * @param item Aladin API 응답 아이템
     * @return 저자 목록 (이름 + 역할)
     */
    List<AuthorWithRole> extract(AladinItemDto item);
}
