package com.nhnacademy.book_data_batch.domain.service.author.parser.strategy;

import com.nhnacademy.book_data_batch.domain.service.author.parser.constant.AuthorRoleMap;
import com.nhnacademy.book_data_batch.domain.service.author.parser.dto.ParsedAuthor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Component
@Order(4)
public class BracketRoleStrategy implements AuthorParsingStrategy {

    @Override
    public List<ParsedAuthor> parse(String input) {
        // 1. 전처리: 양끝 대괄호 제거 로직 단순화
        // 기존 로직 유지: [Role: Name] 형태일 때만 제거
        String processedInput = input;
        if (input.startsWith("[") && input.endsWith("]")) {
            int colonIndex = input.indexOf(':');
            int closeBracketIndex = input.indexOf(']');
            
            // [Role: Name] -> 콜론이 닫는 대괄호보다 앞에 있거나(Role:), 닫는 대괄호가 끝에만 있는 경우
            // 주의: [Role]: Name 은 여기서 제거하면 안 됨.
            if (colonIndex != -1 && (closeBracketIndex == input.length() - 1 || colonIndex < closeBracketIndex)) {
                 processedInput = input.substring(1, input.length() - 1);
            }
        }

        // 2. 첫 번째 역할 분리
        int firstColonIndex = processedInput.indexOf(':');
        if (firstColonIndex == -1) {
            return Collections.emptyList();
        }

        String initialRolePart = processedInput.substring(0, firstColonIndex).trim();
        // 대괄호 제거
        initialRolePart = initialRolePart.replace("[", "").replace("]", "");
        
        String initialRole = AuthorRoleMap.getNormalizedRole(initialRolePart);
        if (initialRole == null) return Collections.emptyList();

        List<ParsedAuthor> authors = new ArrayList<>();
        String namesPart = processedInput.substring(firstColonIndex + 1).trim();
        
        // 3. 쉼표로 이름 분리 및 중간 역할 변경 처리
        String[] parts = namesPart.split(",");
        String currentRole = initialRole;

        for (String part : parts) {
            String trimmedPart = part.trim();
            if (trimmedPart.isBlank()) continue;

            // 중간에 역할이 바뀌는지 확인 (예: ", 엮은이: 홍길동")
            int subColonIndex = trimmedPart.indexOf(':');
            if (subColonIndex != -1) {
                String subRolePart = trimmedPart.substring(0, subColonIndex).trim();
                subRolePart = subRolePart.replace("[", "").replace("]", "");
                
                String newRole = AuthorRoleMap.getNormalizedRole(subRolePart);
                if (newRole != null) {
                    currentRole = newRole;
                    trimmedPart = trimmedPart.substring(subColonIndex + 1).trim();
                }
            }
            
            // 이름에 남은 대괄호 제거
            if (trimmedPart.startsWith("[") && trimmedPart.endsWith("]")) {
                trimmedPart = trimmedPart.substring(1, trimmedPart.length() - 1);
            } else if (trimmedPart.endsWith("]")) {
                 if (!trimmedPart.contains("[")) {
                     trimmedPart = trimmedPart.substring(0, trimmedPart.length() - 1);
                 }
            }

            authors.add(new ParsedAuthor(trimmedPart, currentRole));
        }

        return authors;
    }
}
