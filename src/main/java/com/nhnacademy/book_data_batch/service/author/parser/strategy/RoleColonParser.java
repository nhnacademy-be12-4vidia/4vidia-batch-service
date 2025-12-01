package com.nhnacademy.book_data_batch.service.author.parser.strategy;

import com.nhnacademy.book_data_batch.service.author.dto.AuthorWithRole;
import com.nhnacademy.book_data_batch.service.author.dto.ParseResult;
import com.nhnacademy.book_data_batch.service.author.parser.AuthorRoleParser;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** "역할: 이름" 패턴 */
@Component
public class RoleColonParser implements AuthorRoleParser {

    private static final int PRIORITY = 20;

    private static final Map<String, String> ROLE_MAP = new LinkedHashMap<>();

    static {
        // 연구 관련
        ROLE_MAP.put("연구 책임자", "연구책임자");
        ROLE_MAP.put("연구책임자", "연구책임자");
        ROLE_MAP.put("연구책임", "연구책임자");
        ROLE_MAP.put("책임 연구자", "연구책임자");
        ROLE_MAP.put("책임연구자", "연구책임자");
        ROLE_MAP.put("연구 기관", "연구기관");
        ROLE_MAP.put("연구기관", "연구기관");
        ROLE_MAP.put("[연구기관]", "연구기관");
        ROLE_MAP.put("주관연구기관명", "연구기관");
        ROLE_MAP.put("연구원", "연구원");

        // 저자 관련
        ROLE_MAP.put("저자", "지은이");
        ROLE_MAP.put("지은이", "지은이");
        ROLE_MAP.put("글쓴이", "지은이");
        ROLE_MAP.put("집필", "지은이");
        ROLE_MAP.put("집필인", "지은이");
        ROLE_MAP.put("집필위원회", "지은이");
        ROLE_MAP.put("집필·검토", "지은이");
        ROLE_MAP.put("원고", "지은이");
        ROLE_MAP.put("글", "지은이");
        ROLE_MAP.put("저", "지은이");

        // 그림
        ROLE_MAP.put("그림", "그림");
        ROLE_MAP.put("일러스트", "그림");
        ROLE_MAP.put("삽화", "그림");
        ROLE_MAP.put("만화", "만화");

        // 번역
        ROLE_MAP.put("옮김", "옮긴이");
        ROLE_MAP.put("옮긴이", "옮긴이");
        ROLE_MAP.put("번역", "옮긴이");
        ROLE_MAP.put("역자", "옮긴이");

        // 편집
        ROLE_MAP.put("편집", "엮은이");
        ROLE_MAP.put("엮은이", "엮은이");
        ROLE_MAP.put("엮음", "엮은이");
        ROLE_MAP.put("편", "엮은이");
        ROLE_MAP.put("조사·편집", "엮은이");
        ROLE_MAP.put("조사편찬", "엮은이");

        // 기타
        ROLE_MAP.put("감수", "감수");
        ROLE_MAP.put("감역", "감역");
        ROLE_MAP.put("사진", "사진");
        ROLE_MAP.put("기획", "기획");
        ROLE_MAP.put("원작", "원작");
    }

    private static final Pattern SEGMENT_SPLITTER = Pattern.compile("[;；]");
    private static final Pattern ROLE_COLON_PATTERN;

    static {
        String roles = ROLE_MAP.keySet().stream()
                .sorted((a, b) -> b.length() - a.length())
                .map(Pattern::quote)
                .collect(java.util.stream.Collectors.joining("|"));
        ROLE_COLON_PATTERN = Pattern.compile("(?:\\[)?(" + roles + ")(?:\\])?\\s*[:：]\\s*(.+)");
    }

    @Override
    public ParseResult parse(String authorField) {
        if (!canParse(authorField)) {
            return ParseResult.failure();
        }

        List<AuthorWithRole> result = new ArrayList<>();
        String[] segments = SEGMENT_SPLITTER.split(authorField);

        for (String segment : segments) {
            String trimmed = segment.trim();
            if (trimmed.isBlank()) continue;

            Matcher matcher = ROLE_COLON_PATTERN.matcher(trimmed);
            if (matcher.matches()) {
                String roleKey = matcher.group(1).trim();
                String namePart = matcher.group(2).trim();
                String role = ROLE_MAP.getOrDefault(roleKey, roleKey);

                for (String name : namePart.split("[,;·]")) {
                    String cleaned = cleanName(name);
                    if (isValidName(cleaned)) {
                        result.add(AuthorWithRole.of(cleaned, role));
                    }
                }
            } else if (isValidName(cleanName(trimmed))) {
                // 역할 없는 세그먼트는 지은이로
                result.add(AuthorWithRole.of(cleanName(trimmed), "지은이"));
            }
        }

        return result.isEmpty() ? ParseResult.failure() : ParseResult.success(result, name());
    }

    @Override
    public int priority() {
        return PRIORITY;
    }

    private boolean canParse(String input) {
        if (input == null || input.isBlank()) return false;
        return ROLE_MAP.keySet().stream()
                .anyMatch(role -> input.contains(role + ":") || 
                                  input.contains(role + " :") || 
                                  input.contains(role + "："));
    }

    private String cleanName(String name) {
        if (name == null) return "";
        return name.trim().replaceAll("^[\\s,;:：]+|[\\s,;:：]+$", "");
    }

    private boolean isValidName(String name) {
        return name != null && name.length() >= 2 && 
               !name.matches("^[^가-힣a-zA-Z\\u4e00-\\u9fff]+$");
    }
}
