package com.nhnacademy.book_data_batch.service;

import com.nhnacademy.book_data_batch.batch.book.processor.BookItemProcessor;
import com.nhnacademy.book_data_batch.batch.book.tasklet.ReferenceDataLoadTasklet;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * AuthorNameExtractor
 * - CSV의 저자 문자열에서 유효한 저자명만 추출
 * - 역할 키워드 제거
 * - 구분자(;,:|·) 기반 분리
 *
 * [수정 예정]
 * - 지금은 역할만 걸러서 작가에 넣고
 * - BookAuthor 역할 필드는 모두 "참여자"로 통일
 * - 추후 역할 필드도 분리하여 저장하도록 개선 필요
 * - 패턴 매칭으로 대부분의 케이스를 처리할 수 있게 할 예정
 * 
 * @see ReferenceDataLoadTasklet CSV 전체 스캔 시 사용
 * @see BookItemProcessor Book 데이터 처리 시 사용
 */
@Slf4j
@Service
public class AuthorNameExtractor {

    private static final Pattern PARENTHESES_PATTERN = Pattern.compile("\\([^)]*\\)");

    private static final Set<String> ROLE_KEYWORDS = Set.of(

            "지은이", "지음", "글", "저", "저자", "편", "편저", "편집", "엮은이", "엮음",
            "옮긴이", "옮김", "역", "역자", "번역", "역주", "감역", "감수", "감독",
            "그림", "일러스트", "사진", "원작", "원저", "각색", "각본", "원고", "제작",
            "낭독", "해설", "해제", "주", "주석", "기획", "구성", "연출", "연주", "출연",
            "연구책임자", "연구원", "연구소", "연구기관", "참고서", "공저", "글씨", "외저",
            "[공]저", "[공]역", "[편]", "주최", "[지음]", "[공]편역",
            "글.그림", "[공]편집", "[공]그림", "만화", "[외]글", "[공]지음", "편주",
            "[공]편저", "[저]", "[공]연구", "저자/감수",
            // 일단 대괄호는 수동으로 넣어줌 (수정 필요)

            "author", "authors", "writer", "writers", "by", "written by", "written",
            "editor", "editors", "edited", "edited by", "compiler", "compiled by", "introduction",
            "translator", "translators", "translated by", "translation",
            "illustrator", "illustrators", "illustrated by", "illustrated", "essays by",
            "photographer", "photographed by", "director", "narrator", "(eds.)"
    );

    /**
     * 작가/출판사 추출 단계에서 중복 없는 작가 이름 집합 추출
     * 
     * @param rawAuthor CSV의 원본 저자 문자열
     * @return 중복 제거된 저자명 집합 (빈 입력 시 빈 Set)
     */
    public Set<String> extractUniqueAuthors(String rawAuthor) {
        if (!StringUtils.hasText(rawAuthor)) {
            return Collections.emptySet();
        }

        List<String> names = extractAuthorNames(rawAuthor);

        return new HashSet<>(names);
    }

    /**
     * Book 파싱용: 작가명 목록을 List로 추출
     * - BookItemProcessor에서 개별 도서 처리 시 사용
     * - 순서가 보존된 작가명 목록 반환
     * 
     * [처리 단계]
     * 1. 괄호 내용 중 역할 키워드만 제거
     * 2. 구분자로 토큰 분리
     * 3. 각 토큰에서 역할 키워드 단어 제거
     * 4. 유효성 검사 (2자 이상, 문자 포함)
     * 
     * @param rawAuthor CSV의 원본 저자 문자열
     * @return 저자명 목록 (빈 입력 시 빈 List)
     */
    public List<String> extractAuthorNames(String rawAuthor) {
        if (!StringUtils.hasText(rawAuthor)) {
            return Collections.emptyList();
        }

        List<String> authorNames = new ArrayList<>();

        // 1. 괄호 제거 (역할 정보 제거)
        String cleaned = removeParentheses(rawAuthor);

        // 2. 구분자로 토큰화
        String[] tokens = cleaned.split("[,;:|·]");

        for (String token : tokens) {
            // 3. 공백으로 다시 분리
            String[] words = token.trim().split("\\s+");

            StringBuilder nameBuilder = new StringBuilder();
            for (String word : words) {
                String normalized = word.trim().toLowerCase();

                // 역할 키워드가 아닌 경우만 추가
                if (!normalized.isEmpty() && !isRoleKeyword(normalized)) {
                    if (!nameBuilder.isEmpty()) {
                        nameBuilder.append(" ");
                    }
                    nameBuilder.append(word.trim());
                }
            }

            String extractedName = nameBuilder.toString().trim();

            // 4. 유효성 검사
            if (isValidAuthorName(extractedName)) {
                authorNames.add(extractedName);
            }
        }

        return authorNames;
    }

    /**
     * 괄호와 내용을 조건부 제거
     * - 괄호 내용이 역할 키워드면 → 괄호 전체 제거
     * - 괄호 내용이 역할 키워드가 아니면 → 유지
     * 
     * @param text 원본 문자열
     * @return 괄호 처리된 문자열
     */
    private String removeParentheses(String text) {
        Matcher matcher = PARENTHESES_PATTERN.matcher(text);
        StringBuffer result = new StringBuffer();

        while (matcher.find()) {
            String match = matcher.group();
            String content = match.substring(1, match.length() - 1).trim();
            matcher.appendReplacement(result, isRoleKeyword(content.toLowerCase()) ? "" : match);
        }
        matcher.appendTail(result);
        return result.toString();
    }

    /**
     * 주어진 토큰이 역할 키워드인지 판단
     * 1. ROLE_KEYWORDS에 포함된 경우 → true
     * 2. 1글자 이하인 경우 → true (의미 없는 토큰)
     * 3. 한글/영문이 전혀 없는 경우 → true (숫자, 특수문자만)
     * 
     * @param token 검사할 토큰
     * @return 역할 키워드이면 true
     */
    private boolean isRoleKeyword(String token) {
        if (token == null || token.isEmpty()) {
            return false;
        }

        String normalized = token.toLowerCase().trim();

        // ROLE_KEYWORDS에 포함되면 역할 키워드
        if (ROLE_KEYWORDS.contains(normalized)) {
            return true;
        }

        // 1글자 이하는 의미 없는 토큰으로 간주
        if (normalized.length() <= 1) {
            return true;
        }

        // 한글/영문이 없으면 유효한 이름이 아님
        if (normalized.matches("^[^가-힣a-zA-Z]+$")) {
            return true;
        }

        return false;
    }

    /**
     * 추출된 이름이 유효한 저자명인지 검증
     * - 최소 2자 이상
     * - 역할 키워드가 아님
     * - 한글 또는 영문이 최소 1자 이상 포함
     * 
     * @param name 검증할 이름
     * @return 유효한 저자명이면 true
     */
    private boolean isValidAuthorName(String name) {
        if (name == null || name.trim().isEmpty() || name.length() < 2) {
            return false;
        }

        if (isRoleKeyword(name.toLowerCase())) {
            return false;
        }

        // 한글/영문이 없으면 유효하지 않음
        if (name.matches("^[^가-힣a-zA-Z]+$")) {
            return false;
        }

        return true;
    }
}
