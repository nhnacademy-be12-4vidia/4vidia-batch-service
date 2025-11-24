package com.nhnacademy.book_data_batch.batch.book.resolver;

import com.nhnacademy.book_data_batch.batch.book.dto.AuthorRole;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 도서의 저자 및 역할 정보를 파싱하는 유틸리티 클래스
 * 저자 필드에서 여러 저자와 그 역할을 추출 -> AuthorRole 객체 리스트로 반환
 *
 * 근데 너무 어렵다. 으악! 살려줘! 살려줘! 살려줘!
 * 어려운 이유
 * 1. 역할 이름이 너무 많음
 * 2. 괄호 종류가 다양함 (있는 것도 있고 없는 것도 있고, (), [], {},  등등)
 * 3. 구분자도 다양함 (쉼표, 슬래시, 공백, 중간점 등등)
 * 4. 역할이 앞에 오기도 하고 뒤에 오기도 함
 * 5. 역할이 아예 없는 경우도 있음
 * 으악! 으악! 으앆!
 *
 * 그래서 일단 미루고 나중에 해야지
 * - 작자미상 / 지은이 로 고정함
 *
 * 지금 생각나는 해결 방법
 * - 일단 많이 나오는 패턴들 처리 (이름 (역할), 이름 [역할], 역할: 이름 등)
 * - 패턴으로 처리 안 된거 따로 모아서 작가를 명확하게 주는 api들 불러서 처리
 */
@Component
public class AuthorRoleResolver {
    private static final String DEFAULT_NAME = "작자미상";
    private static final String DEFAULT_ROLE = "지은이";

    // 주요 메서드: 저자 필드 파싱
    public List<AuthorRole> parse(String author, String authorSearch) {
        return new ArrayList<>(Collections.singleton(new AuthorRole(DEFAULT_NAME, DEFAULT_ROLE)));
    }
}
