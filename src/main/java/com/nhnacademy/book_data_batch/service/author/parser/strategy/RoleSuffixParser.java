package com.nhnacademy.book_data_batch.service.author.parser.strategy;

import com.nhnacademy.book_data_batch.service.author.dto.AuthorWithRole;
import com.nhnacademy.book_data_batch.service.author.dto.ParseResult;
import com.nhnacademy.book_data_batch.service.author.parser.AuthorRoleParser;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** "이름 역할" 패턴 */
@Component
public class RoleSuffixParser implements AuthorRoleParser {

    private static final int PRIORITY = 40;

    // 일반 역할 접미사
    private static final Set<String> ROLE_SUFFIXES = Set.of(
            "편", "지음", "저", "공저", "글", "그림", "옮김", "역",
            "지은이", "옮긴이", "엮은이", "편저", "편집", "엮음",
            "감수", "감역", "원작", "원저", "사진", "기획", "만화", "낭독",
            "글·그림", "글.그림", "글,그림", "글/그림"
    );

    // 대괄호 역할 접미사
    private static final Map<String, String> BRACKETED_ROLES = new LinkedHashMap<>();

    static {
        BRACKETED_ROLES.put("[외]지음", "지은이");
        BRACKETED_ROLES.put("[공]지음", "지은이");
        BRACKETED_ROLES.put("[공]저", "지은이");
        BRACKETED_ROLES.put("[외]저", "지은이");
        BRACKETED_ROLES.put("[저]", "지은이");
        BRACKETED_ROLES.put("[외]글", "지은이");
        BRACKETED_ROLES.put("[공]글", "지은이");
        BRACKETED_ROLES.put("[같이]편", "엮은이");
        BRACKETED_ROLES.put("[공]편", "엮은이");
        BRACKETED_ROLES.put("[외]편", "엮은이");
        BRACKETED_ROLES.put("[편]", "엮은이");
        BRACKETED_ROLES.put("[외]엮음", "엮은이");
        BRACKETED_ROLES.put("[공]엮음", "엮은이");
        BRACKETED_ROLES.put("[공]편저", "엮은이");
        BRACKETED_ROLES.put("[외]편저", "엮은이");
        BRACKETED_ROLES.put("[공]편역", "엮은이");
        BRACKETED_ROLES.put("[공]편집", "엮은이");
        BRACKETED_ROLES.put("[공]역", "옮긴이");
        BRACKETED_ROLES.put("[외]역", "옮긴이");
        BRACKETED_ROLES.put("[역]", "옮긴이");
        BRACKETED_ROLES.put("[외]옮김", "옮긴이");
        BRACKETED_ROLES.put("[공]옮김", "옮긴이");
        BRACKETED_ROLES.put("[공]그림", "그림");
        BRACKETED_ROLES.put("[외]그림", "그림");
        BRACKETED_ROLES.put("[공]연구", "연구");
    }

    private static final Pattern SEGMENT_SPLITTER = Pattern.compile("[;；]");
    private static final Pattern ROLE_SUFFIX_PATTERN;
    private static final Pattern BRACKETED_PATTERN;

    static {
        String roles = ROLE_SUFFIXES.stream()
                .map(Pattern::quote)
                .collect(java.util.stream.Collectors.joining("|"));
        ROLE_SUFFIX_PATTERN = Pattern.compile("(.+?)\\s+(" + roles + ")$");

        String bracketed = BRACKETED_ROLES.keySet().stream()
                .map(Pattern::quote)
                .collect(java.util.stream.Collectors.joining("|"));
        BRACKETED_PATTERN = Pattern.compile("(.+?)\\s*(" + bracketed + ")\\s*$");
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

            // 대괄호 패턴 먼저 시도
            Matcher bracketMatcher = BRACKETED_PATTERN.matcher(trimmed);
            if (bracketMatcher.matches()) {
                String namesPart = bracketMatcher.group(1).trim();
                String roleKey = bracketMatcher.group(2);
                String role = BRACKETED_ROLES.getOrDefault(roleKey, "지은이");
                addNames(result, namesPart, role);
                continue;
            }

            // 일반 역할 접미사 패턴
            Matcher suffixMatcher = ROLE_SUFFIX_PATTERN.matcher(trimmed);
            if (suffixMatcher.matches()) {
                String namesPart = suffixMatcher.group(1).trim();
                String role = normalizeRole(suffixMatcher.group(2).trim());
                addNames(result, namesPart, role);
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
        // 괄호 패턴은 StandardParenthesesParser가 처리
        if (input.contains("(") && input.contains(")")) return false;
        
        // 대괄호 역할 패턴
        if (BRACKETED_ROLES.keySet().stream().anyMatch(input::contains)) return true;
        
        // 세미콜론 구분자가 있고 역할 키워드가 있는 경우
        if (input.contains(";")) {
            return ROLE_SUFFIX_PATTERN.matcher(input.split(";")[0].trim()).find() ||
                   ROLE_SUFFIXES.stream().anyMatch(r -> input.contains(" " + r));
        }
        
        return ROLE_SUFFIX_PATTERN.matcher(input.trim()).matches();
    }

    private void addNames(List<AuthorWithRole> result, String namesPart, String role) {
        for (String name : namesPart.split("[,;·，]")) {
            String cleaned = name.trim().replaceAll("^[\\s,;:]+|[\\s,;:]+$", "");
            if (isValidName(cleaned)) {
                result.add(AuthorWithRole.of(cleaned, role));
            }
        }
    }

    private boolean isValidName(String name) {
        return name != null && name.length() >= 2 &&
               !name.matches("^[^가-힣a-zA-Z\\u4e00-\\u9fff]+$");
    }

    private String normalizeRole(String role) {
        String first = role.split("[·.,/]")[0].trim().replaceAll("[\\[\\]]", "");
        return switch (first) {
            case "지음", "글", "저", "공저" -> "지은이";
            case "옮김", "역", "공역" -> "옮긴이";
            case "엮음", "편", "편집", "편저", "공편" -> "엮은이";
            default -> first;
        };
    }
}
