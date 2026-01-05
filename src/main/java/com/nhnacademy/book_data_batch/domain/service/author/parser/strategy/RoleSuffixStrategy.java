package com.nhnacademy.book_data_batch.domain.service.author.parser.strategy;

import com.nhnacademy.book_data_batch.domain.service.author.parser.constant.AuthorRoleMap;
import com.nhnacademy.book_data_batch.domain.service.author.parser.dto.ParsedAuthor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Component
@Order(5)
public class RoleSuffixStrategy implements AuthorParsingStrategy {

    @Override
    public List<ParsedAuthor> parse(String input) {
        if (input == null || input.isBlank()) {
            return Collections.emptyList();
        }

        int lastSpaceIndex = input.lastIndexOf(' ');
        if (lastSpaceIndex == -1) {
            return Collections.emptyList();
        }

        // 마지막 공백을 기준으로 이름부와 역할부 분리
        String roleCandidate = input.substring(lastSpaceIndex + 1).trim();
        String globalRole = AuthorRoleMap.getNormalizedRole(roleCandidate);
        
        if (globalRole == null) {
            return Collections.emptyList();
        }

        String namesPart = input.substring(0, lastSpaceIndex).trim();
        // 이름 앞뒤에 대괄호가 있으면 제거
        namesPart = namesPart.replaceAll("^\\[", "").replaceAll("]$", "");

        List<String> names = Arrays.stream(namesPart.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();

        if (names.isEmpty()) return Collections.emptyList();

        List<ParsedAuthor> results = new ArrayList<>();

        for (String name : names) {
            // 개별 이름에 역할이 포함되어 있는지 확인 (재귀적 로직 대신 간단한 suffix 체크)
            // 개별 이름 파싱 로직도 lastIndexOf로 처리
            int localSpaceIndex = name.lastIndexOf(' ');
            if (localSpaceIndex != -1) {
                String localRoleRaw = name.substring(localSpaceIndex + 1).trim();
                String localRole = AuthorRoleMap.getNormalizedRole(localRoleRaw);

                if (localRole != null) {
                    String realName = name.substring(0, localSpaceIndex).trim();
                    results.add(new ParsedAuthor(realName, localRole));

                    // 이름이 하나뿐이고, 글로벌 역할도 있다면 둘 다 적용 (예: "이름 글, 그림")
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
}
