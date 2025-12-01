package com.nhnacademy.book_data_batch.service.author.dto;

import java.util.List;

/**
 * 작가 파싱 결과를 담는 불변 DTO
 *
 * @param authors    파싱된 작가-역할 목록
 * @param success    파싱 성공 여부
 * @param strategyName 사용된 전략 이름 (디버깅/통계용)
 */
public record ParseResult(
        List<AuthorWithRole> authors,
        boolean success,
        String strategyName
) {
    private static final ParseResult FAILURE = new ParseResult(List.of(), false, "none");

    /**
     * 파싱 실패 결과 (싱글턴)
     */
    public static ParseResult failure() {
        return FAILURE;
    }

    /**
     * 파싱 성공 결과 생성
     */
    public static ParseResult success(List<AuthorWithRole> authors, String strategyName) {
        if (authors == null || authors.isEmpty()) {
            return failure();
        }
        return new ParseResult(List.copyOf(authors), true, strategyName);
    }

    /**
     * 파싱된 작가 수
     */
    public int authorCount() {
        return authors.size();
    }
}
