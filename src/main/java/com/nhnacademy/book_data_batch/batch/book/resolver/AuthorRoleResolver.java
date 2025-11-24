package com.nhnacademy.book_data_batch.batch.book.resolver;

import com.nhnacademy.book_data_batch.batch.book.dto.AuthorRole;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 도서의 저자 및 역할 정보를 파싱하는 유틸리티 클래스
 * 저자 필드에서 여러 저자와 그 역할을 추출 -> AuthorRole 객체 리스트로 반환
 */
@Component
public class AuthorRoleResolver {

    private static final Pattern ROLE_BRACKET = Pattern.compile(
            "(\\(|\\[)([^()\\[\\]]+?)(\\)|\\])"
    );
    private static final Pattern TRAILING_ROLE = Pattern.compile(
            "(?:\\s|·|,|/|-)+(지음|지은이|저자|글|글쓴이|집필|옮김|옮긴이|역|역자|번역|엮음|엮은이|편집|편|편저|감수|그림|그린이|일러스트|사진|촬영|기획|연구|주해|주석|해설|구성|정리)\\s*$"
    );
    private static final Pattern LEADING_INDICATOR = Pattern.compile(
            "(?i)^(?:by|text by|story by|words by|illustrated by|photo by|photos by|compiled by|edited by)[:\\s]+"
    );
    private static final String defaultAuthor = "작자미상";
    private static final String defaultRole = "지은이";

    // 주요 메서드: 저자 필드 파싱
    public List<AuthorRole> parse(String authorField) {
        if (!StringUtils.hasText(authorField)) {
            return List.of(fallbackAuthorRole());
        }

        String normalized = normalizeAuthorField(authorField);
        List<String> tokens = splitAuthorTokens(normalized);
        Map<String, AuthorRole> unique = new LinkedHashMap<>();

        for (String token : tokens) {
            AuthorRole resolved = resolveAuthorRole(token);
            if (resolved == null) {
                continue;
            }
            AuthorRole existing = unique.get(resolved.name());
            if (existing == null) {
                unique.put(resolved.name(), resolved);
                continue;
            }
            String mergedRole = mergeRoleLabels(existing.role(), resolved.role());
            if (!mergedRole.equals(existing.role())) {
                unique.put(resolved.name(), AuthorRole.builder()
                        .name(existing.name())
                        .role(mergedRole)
                        .build());
            }
        }

        if (unique.isEmpty()) {
            return List.of(fallbackAuthorRole());
        }
        return new ArrayList<>(unique.values());
    }

    // 기본 저자 역할 반환
    private AuthorRole fallbackAuthorRole() {
        return AuthorRole.builder()
                .name(defaultAuthor)
                .role(defaultRole)
                .build();
    }

    // 저자 필드 정규화
    private String normalizeAuthorField(String authorField) {
        String normalized = authorField
                .replace('\r', ' ')
                .replace('\n', ' ')
                .replace('／', '/')
                .replace('＆', '&')
                .replace('︱', '-')
                .replace('ㆍ', '·');
        return normalized.replaceAll("\\s+", " ").trim();
    }

    // 저자 토큰 분리
    private List<String> splitAuthorTokens(String normalized) {
        List<String> tokens = new ArrayList<>();
        if (!StringUtils.hasText(normalized)) {
            return tokens;
        }

        StringBuilder current = new StringBuilder();
        int depth = 0;
        for (int i = 0; i < normalized.length(); i++) {
            char ch = normalized.charAt(i);
            if (ch == '(' || ch == '[') {
                depth++;
            } else if (ch == ')' || ch == ']') {
                if (depth > 0) {
                    depth--;
                }
            }

            if (depth == 0 && isAuthorDelimiter(ch)) {
                appendToken(current, tokens);
            } else {
                current.append(ch);
            }
        }
        appendToken(current, tokens);
        return tokens;
    }

    // 저자 구분자 확인
    private boolean isAuthorDelimiter(char ch) {
        return ch == ',' || ch == '/' || ch == ';' || ch == '&';
    }

    // 토큰 추가
    private void appendToken(StringBuilder builder, List<String> tokens) {
        String value = builder.toString().trim();
        if (StringUtils.hasText(value)) {
            tokens.add(value);
        }
        builder.setLength(0);
    }

    // 개별 저자 역할 파싱
    private AuthorRole resolveAuthorRole(String rawToken) {
        if (!StringUtils.hasText(rawToken)) {
            return null;
        }

        String working = rawToken.trim();
        List<String> roles = new ArrayList<>();

        working = capturePrefixRole(working, roles);
        working = captureBracketRoles(working, roles);
        working = captureTrailingRole(working, roles);

        String name = cleanupName(working);
        if (!StringUtils.hasText(name)) {
            return null;
        }

        String resolvedRole = formatRoleLabel(roles);
        return AuthorRole.builder()
                .name(name)
                .role(resolvedRole)
                .build();
    }

    // 접두사 역할 캡처
    private String capturePrefixRole(String token, List<String> roles) {
        int colonIndex = indexOfColon(token);
        if (colonIndex <= 0) {
            return token;
        }
        String prefix = token.substring(0, colonIndex).trim();
        if (StringUtils.hasText(prefix)) {
            roles.add(prefix);
            return token.substring(colonIndex + 1).trim();
        }
        return token;
    }

    // 콜론 위치 찾기
    private int indexOfColon(String text) {
        int ascii = text.indexOf(':');
        int fullWidth = text.indexOf('：');
        if (ascii == -1) {
            return fullWidth;
        }
        if (fullWidth == -1) {
            return ascii;
        }
        return Math.min(ascii, fullWidth);
    }

    // 괄호 내 역할 캡처
    private String captureBracketRoles(String token, List<String> roles) {
        Matcher matcher = ROLE_BRACKET.matcher(token);
        StringBuffer result = new StringBuffer();
        while (matcher.find()) {
            addRoleCandidates(matcher.group(2), roles);
            matcher.appendReplacement(result, "");
        }
        matcher.appendTail(result);
        return result.toString().trim();
    }

    // 후행 역할 캡처
    private String captureTrailingRole(String token, List<String> roles) {
        Matcher matcher = TRAILING_ROLE.matcher(token);
        if (!matcher.find()) {
            return token;
        }
        addRoleCandidates(matcher.group(1), roles);
        return token.substring(0, matcher.start()).trim();
    }

    // 역할 후보 추가
    private void addRoleCandidates(String block, List<String> roles) {
        if (!StringUtils.hasText(block)) {
            return;
        }
        String cleaned = block.replace('·', ',').replace('ㆍ', ',')
                .replace("및", ",")
                .replace("그리고", ",");
        for (String part : cleaned.split("[,/;&]")) {
            String role = part.trim();
            if (StringUtils.hasText(role)) {
                roles.add(role);
            }
        }
    }

    // 이름 정리
    private String cleanupName(String candidate) {
        if (!StringUtils.hasText(candidate)) {
            return null;
        }
        String cleaned = candidate
                .replaceAll("\\s+", " ")
                .replaceAll("^[\\p{Punct}\\s]+", "")
                .replaceAll("[\\p{Punct}\\s]+$", "")
                .trim();
        cleaned = stripLeadingIndicators(cleaned);
        return StringUtils.hasText(cleaned) ? cleaned : null;
    }

    // 선행 지시어 제거
    private String stripLeadingIndicators(String value) {
        if (!StringUtils.hasText(value)) {
            return value;
        }
        String trimmed = value.trim();
        Matcher matcher = LEADING_INDICATOR.matcher(trimmed);
        while (matcher.find()) {
            trimmed = trimmed.substring(matcher.end()).trim();
            matcher = LEADING_INDICATOR.matcher(trimmed);
        }
        return trimmed;
    }

    // 역할 라벨 포맷팅
    private String formatRoleLabel(List<String> roles) {
        if (roles == null || roles.isEmpty()) {
            return defaultRole;
        }
        return mergeRoleLabels(roles.toArray(new String[0]));
    }

    // 역할 라벨 병합
    private String mergeRoleLabels(String... labels) {
        LinkedHashSet<String> unique = new LinkedHashSet<>();
        if (labels != null) {
            for (String label : labels) {
                if (!StringUtils.hasText(label)) {
                    continue;
                }
                String[] parts = label.split("\\s*,\\s*");
                for (String part : parts) {
                    if (StringUtils.hasText(part)) {
                        unique.add(part.trim());
                    }
                }
            }
        }
        return unique.isEmpty() ? defaultRole : String.join(", ", unique);
    }
}
