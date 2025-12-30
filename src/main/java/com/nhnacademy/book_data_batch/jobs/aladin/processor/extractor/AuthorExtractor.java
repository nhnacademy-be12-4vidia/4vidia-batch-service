package com.nhnacademy.book_data_batch.jobs.aladin.processor.extractor;

import com.nhnacademy.book_data_batch.jobs.aladin.dto.api.AladinBookInfoDto;
import com.nhnacademy.book_data_batch.jobs.aladin.dto.EnrichmentSuccessDto.AuthorWithRole;
import com.nhnacademy.book_data_batch.jobs.aladin.dto.api.AladinItemDto;
import com.nhnacademy.book_data_batch.domain.service.author.parser.AuthorParser;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;

/**
 * Aladin API 응답에서 정규화된 저자 정보 추출
 */
@Component
@RequiredArgsConstructor
public class AuthorExtractor {

    private static final String DEFAULT_ROLE = "지은이";
    private final AuthorParser authorParser;

    public List<AuthorWithRole> extract(AladinItemDto item) {
        if (item == null) {
            return Collections.emptyList();
        }

        AladinBookInfoDto bookinfo = item.bookinfo();

        // 1. bookinfo.authors (정규화된 데이터)
        if (hasStructuredAuthors(bookinfo)) {
            return extractFromBookInfo(bookinfo);
        }

        // 2. item.author 문자열 파싱
        // 파싱 실패 시, 원본 필드를 이름으로, 역할을 null로 하는 단일 저자 반환
        // 먼 미래에 시간이 된다면, 배치로 null 역할이나 복합 역할을 정규화하는 llm 작업 할 수 있게..
        if (StringUtils.hasText(item.author())) {
            return parseAuthorString(item.author());
        }

        return Collections.emptyList();
    }

    // 정규 작가 필드가 존재하는지 확인
    private boolean hasStructuredAuthors(AladinBookInfoDto bookinfo) {
        return bookinfo != null
                && bookinfo.authors() != null
                && !bookinfo.authors().isEmpty();
    }

    // 정규 작가 필드 추출
    private List<AuthorWithRole> extractFromBookInfo(AladinBookInfoDto bookinfo) {
        return bookinfo.authors().stream()
                .filter(a -> StringUtils.hasText(a.name()))
                .map(a -> new AuthorWithRole(
                        a.name().trim(),
                        StringUtils.hasText(a.desc()) ? a.desc().trim() : DEFAULT_ROLE
                ))
                .toList();
    }

    // 비정규 작가 필드 파싱
    private List<AuthorWithRole> parseAuthorString(String author) {
        return authorParser.parse(author).stream()
                .map(parsed -> new AuthorWithRole(parsed.name(), parsed.role()))
                .toList();
    }
}
