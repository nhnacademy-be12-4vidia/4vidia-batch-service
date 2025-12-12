package com.nhnacademy.book_data_batch.service.author.parser.strategy;

import com.nhnacademy.book_data_batch.service.author.parser.constant.AuthorRoleMap;
import com.nhnacademy.book_data_batch.service.author.parser.dto.ParsedAuthor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@Order(5)
public class RoleSuffixStrategy implements AuthorParsingStrategy {
    // "이름 역할" 또는 "이름, 이름 역할" 패턴 매칭
    private static final Pattern PATTERN = Pattern.compile("^(.+?)\\s+(\\S+)$");

    @Override
    public List<ParsedAuthor> parse(String input) {
        Matcher matcher = PATTERN.matcher(input);
        if (matcher.matches()) {
            String globalRole = AuthorRoleMap.getNormalizedRole(matcher.group(2).trim());
            if (globalRole == null) return Collections.emptyList();

            String namesPart = matcher.group(1);
            // 이름 앞뒤에 대괄호가 있으면 제거
            namesPart = namesPart.replaceAll("^\\[", "").replaceAll("]$", "");

            List<String> names = Arrays.stream(namesPart.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .toList();

            if (names.isEmpty()) return Collections.emptyList();

            List<ParsedAuthor> results = new ArrayList<>();

            for (String name : names) {
                // 개별 이름에 역할이 포함되어 있는지 확인
                Matcher nameMatcher = PATTERN.matcher(name);
                if (nameMatcher.matches()) {
                    String localRoleRaw = nameMatcher.group(2).trim();
                    String localRole = AuthorRoleMap.getNormalizedRole(localRoleRaw);

                    if (localRole != null) {
                        String realName = nameMatcher.group(1).trim();
                        results.add(new ParsedAuthor(realName, localRole));

                        // 이름이 하나뿐이고, 글로벌 역할도 있다면 둘 다 적용 (예: "이름 글, 그림")
                        // 단, 역할이 다를 경우에만 추가
                        if (names.size() == 1 && !localRole.equals(globalRole)) {
                            results.add(new ParsedAuthor(realName, globalRole));
                        }
                        continue;
                    }
                }

                // 개별 역할이 없으면 글로벌 역할 적용
                results.add(new ParsedAuthor(name, globalRole));
            }
            return results;
        }
        return Collections.emptyList();
    }
}
