package com.nhnacademy.book_data_batch.batch.aladin.extractor;

import com.nhnacademy.book_data_batch.batch.aladin.dto.AladinBookInfoDto;
import com.nhnacademy.book_data_batch.batch.aladin.dto.AladinItemDto;
import com.nhnacademy.book_data_batch.batch.aladin.dto.EnrichmentResultDto.AuthorWithRole;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;

/**
 * Aladin API 응답에서 저자 정보 추출
 */
@Component
public class AladinAuthorExtractor {

    private static final String DEFAULT_ROLE = "지은이";

    public List<AuthorWithRole> extract(AladinItemDto item) {
        AladinBookInfoDto bookinfo = item.bookinfo();

        // bookinfo.authors
        if (hasDetailedAuthors(bookinfo)) {
            return extractFromBookInfo(bookinfo);
        }

        return Collections.emptyList();
    }

    private boolean hasDetailedAuthors(AladinBookInfoDto bookinfo) {
        return bookinfo != null 
                && bookinfo.authors() != null 
                && !bookinfo.authors().isEmpty();
    }

    private List<AuthorWithRole> extractFromBookInfo(AladinBookInfoDto bookinfo) {
        return bookinfo.authors().stream()
                .filter(a -> StringUtils.hasText(a.authorName()))
                .map(a -> new AuthorWithRole(
                        a.authorName().trim(),
                        StringUtils.hasText(a.authorRole())
                                ? a.authorRole().trim() : DEFAULT_ROLE))
                .toList();
    }
}
