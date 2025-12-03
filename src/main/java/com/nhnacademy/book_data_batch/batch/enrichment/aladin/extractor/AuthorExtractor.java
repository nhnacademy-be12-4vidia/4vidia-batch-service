package com.nhnacademy.book_data_batch.batch.enrichment.aladin.extractor;

import com.nhnacademy.book_data_batch.batch.enrichment.aladin.dto.AladinBookInfoDto;
import com.nhnacademy.book_data_batch.batch.enrichment.aladin.dto.AladinEnrichmentData.AuthorWithRole;
import com.nhnacademy.book_data_batch.batch.enrichment.aladin.dto.AladinItemDto;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;

/**
 * Aladin API 응답에서 정규화된 저자 정보 추출
 */
@Component
public class AuthorExtractor {

    private static final String DEFAULT_ROLE = "지은이";

    public List<AuthorWithRole> extract(AladinItemDto item) {
        if (item == null) {
            return Collections.emptyList();
        }

        AladinBookInfoDto bookinfo = item.bookinfo();

        // 1순위: bookinfo.authors (정규화된 데이터)
        if (hasStructuredAuthors(bookinfo)) {
            return extractFromBookInfo(bookinfo);
        }

        // 2순위: item.author 문자열 파싱
        if (StringUtils.hasText(item.author())) {
            return parseAuthorString(item.author());
        }

        return Collections.emptyList();
    }

    private List<AuthorWithRole> extractFromBookInfo(AladinBookInfoDto bookinfo) {
        return bookinfo.authors().stream()
                .filter(a -> StringUtils.hasText(a.name()))
                .map(a -> new AuthorWithRole(
                        a.name().trim(),
                        StringUtils.hasText(a.desc()) ? a.desc().trim() : DEFAULT_ROLE
                ))
                .toList();
    }

    private boolean hasStructuredAuthors(AladinBookInfoDto bookinfo) {
        return bookinfo != null
                && bookinfo.authors() != null
                && !bookinfo.authors().isEmpty();
    }

    private List<AuthorWithRole> parseAuthorString(String author) {
        // TODO: 작가 문자열 파싱 로직 구현
        return Collections.emptyList();
    }
}
