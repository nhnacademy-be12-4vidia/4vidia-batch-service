package com.nhnacademy.book_data_batch.service.author.parser.strategy;

import com.nhnacademy.book_data_batch.service.author.dto.AuthorWithRole;
import com.nhnacademy.book_data_batch.service.author.dto.ParseResult;
import com.nhnacademy.book_data_batch.service.author.parser.AuthorRoleParser;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** "이름 (역할)" 패턴 (Parentheses: 소괄호라는 뜻:))*/
@Component
public class ParenthesesRoleParser implements AuthorRoleParser {

    private static final int PRIORITY = 60;

    private static final Pattern ROLE_PATTERN = Pattern.compile("\\(([^)]+)\\)");

    private static final Set<String> ROLE_KEYWORDS = Set.of(
            "지은이", "지음", "글", "저", "저자", "편", "편저", "편집", "엮은이", "엮음",
            "옮긴이", "옮김", "역", "역자", "번역", "역주", "감역", "감수", "감독",
            "그림", "일러스트", "사진", "원작", "원저", "각색", "각본", "원고", "제작",
            "낭독", "해설", "해제", "주", "주석", "기획", "구성", "연출", "연주", "출연",
            "연구책임자", "연구원", "연구소", "연구기관", "공저", "글씨", "외저", "만화", "편주",
            "저자/감수", "글.그림", "글·그림", "글, 그림"
    );

    @Override
    public ParseResult parse(String authorField) {
        if (!canParse(authorField)) {
            return ParseResult.failure();
        }

        List<AuthorWithRole> result = new ArrayList<>();
        List<RoleSegment> segments = findRoleSegments(authorField);

        if (segments.isEmpty()) {
            return ParseResult.failure();
        }

        int lastEnd = 0;
        for (RoleSegment seg : segments) {
            String namesPart = authorField.substring(lastEnd, seg.start).trim();
            for (String name : namesPart.split("[,;·]")) {
                String cleaned = cleanName(name);
                if (isValidName(cleaned)) {
                    result.add(AuthorWithRole.of(cleaned, seg.role));
                }
            }
            lastEnd = seg.end;
        }

        return result.isEmpty() ? ParseResult.failure() : ParseResult.success(result, name());
    }

    @Override
    public int priority() {
        return PRIORITY;
    }

    private boolean canParse(String input) {
        if (input == null || input.isBlank()) return false;
        Matcher m = ROLE_PATTERN.matcher(input);
        while (m.find()) {
            if (isRoleKeyword(m.group(1).trim())) return true;
        }
        return false;
    }

    private List<RoleSegment> findRoleSegments(String text) {
        List<RoleSegment> segments = new ArrayList<>();
        Matcher m = ROLE_PATTERN.matcher(text);
        while (m.find()) {
            String content = m.group(1).trim();
            if (isRoleKeyword(content)) {
                segments.add(new RoleSegment(m.start(), m.end(), normalizeRole(content)));
            }
        }
        return segments;
    }

    private boolean isRoleKeyword(String text) {
        if (text == null || text.isBlank()) return false;
        String lower = text.toLowerCase().trim();
        if (ROLE_KEYWORDS.contains(lower)) return true;
        return lower.matches("외\\s*\\d+인?");
    }

    private String normalizeRole(String role) {
        return switch (role.trim()) {
            case "지음", "글", "저" -> "지은이";
            case "옮김", "역", "번역" -> "옮긴이";
            case "엮음", "편", "편집" -> "엮은이";
            default -> role.trim();
        };
    }

    private String cleanName(String name) {
        if (name == null) return "";
        return name.trim().replaceAll("^[,;:·\\s]+|[,;:·\\s]+$", "");
    }

    private boolean isValidName(String name) {
        if (name == null || name.isBlank() || name.length() < 2) return false;
        if (name.matches("^[^가-힣a-zA-Z\\u4e00-\\u9fff]+$")) return false;
        return !isRoleKeyword(name);
    }

    private record RoleSegment(int start, int end, String role) {}
}
