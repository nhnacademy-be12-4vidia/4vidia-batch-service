package com.nhnacademy.book_data_batch.service.author.parser;

import com.nhnacademy.book_data_batch.service.author.dto.ParseResult;

/**
 * 작가-역할 파싱 전략 인터페이스
 */
public interface AuthorRoleParser {

    ParseResult parse(String authorField);

    int priority();

    default String name() {
        return getClass().getSimpleName();
    }
}
