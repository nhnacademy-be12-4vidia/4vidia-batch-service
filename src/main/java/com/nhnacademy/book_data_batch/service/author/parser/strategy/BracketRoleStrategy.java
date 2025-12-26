package com.nhnacademy.book_data_batch.service.author.parser.strategy;

import com.nhnacademy.book_data_batch.service.author.parser.constant.AuthorRoleMap;
import com.nhnacademy.book_data_batch.service.author.parser.dto.ParsedAuthor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@Order(4)
public class BracketRoleStrategy implements AuthorParsingStrategy {
    // "역할: 이름" 또는 "[역할]: 이름" 또는 "[역할: 이름]" 패턴을 감지하기 위한 정규식
    // 그룹 1: 역할 (대괄호 제외)
    // 그룹 2: 나머지 내용 (이름들)
    private static final Pattern START_PATTERN = Pattern.compile("^\\[?([^\\]:]+)\\]?:\\s*(.+)$");
    
    // 중간에 역할이 바뀌는 경우를 감지하기 위한 정규식 (예: ", 엮은이: 권영애")
    private static final Pattern ROLE_CHANGE_PATTERN = Pattern.compile("^\\[?([^\\]:]+)\\]?:\\s*(.+)$");

    @Override
    public List<ParsedAuthor> parse(String input) {
        // 1. 전체 문자열이 "[Role: Name, Name]" 형태인지 확인하고 대괄호 제거
        String processedInput = input;
        if (input.startsWith("[") && input.endsWith("]")) {
            // 내부에도 대괄호가 있을 수 있으므로, 단순히 양끝만 제거하는 것이 안전한지 확인 필요
            // 하지만 "[Role: Name]" 형태를 처리하기 위해 일단 제거 시도
            // 주의: "[Role]: Name" 형태는 여기서 제거하면 안됨.
            // 구분: "[Role: ...]" vs "[Role]: ..."
            // 콜론의 위치로 판단?
            int colonIndex = input.indexOf(':');
            int closeBracketIndex = input.indexOf(']');
            
            // 콜론이 닫는 대괄호보다 뒤에 있거나(Role:), 닫는 대괄호가 없으면(Role: ...)
            // "[Role: Name]" -> 콜론 < 닫는 대괄호 (닫는 대괄호가 끝에 있는 경우)
            // "[Role]: Name" -> 콜론 > 닫는 대괄호
            
            if (colonIndex != -1 && (closeBracketIndex == -1 || colonIndex < closeBracketIndex)) {
                 // [Role: Name] 형태일 가능성 높음 -> 양끝 대괄호 제거
                 processedInput = input.substring(1, input.length() - 1);
            }
        }

        Matcher matcher = START_PATTERN.matcher(processedInput);
        if (!matcher.matches()) {
            return Collections.emptyList();
        }

        String initialRole = AuthorRoleMap.getNormalizedRole(matcher.group(1));
        if (initialRole == null) return Collections.emptyList();

        List<ParsedAuthor> authors = new ArrayList<>();
        String namesPart = matcher.group(2).trim();
        
        // 쉼표로 분리하되, 각 부분이 새로운 역할 정의인지 확인
        String[] parts = namesPart.split(",");
        String currentRole = initialRole;

        for (String part : parts) {
            String trimmedPart = part.trim();
            if (trimmedPart.isBlank()) continue;

            // 새로운 역할 정의인지 확인
            Matcher roleMatcher = ROLE_CHANGE_PATTERN.matcher(trimmedPart);
            if (roleMatcher.matches()) {
                String newRole = AuthorRoleMap.getNormalizedRole(roleMatcher.group(1));
                if (newRole != null) {
                    currentRole = newRole;
                    trimmedPart = roleMatcher.group(2).trim();
                }
            }
            
            // 이름에 대괄호가 남아있으면 제거 (예: "[편: Name]"에서 앞부분 처리 후 "Name]"이 남는 경우 등)
            // 하지만 위에서 processedInput 처리를 했으므로, 여기서는 "[Name]" 형태만 체크
            if (trimmedPart.startsWith("[") && trimmedPart.endsWith("]")) {
                trimmedPart = trimmedPart.substring(1, trimmedPart.length() - 1);
            } else if (trimmedPart.endsWith("]")) {
                 // "[Role: Name]" 처리 시 쉼표로 잘리면서 마지막에 "]"가 남는 경우
                 // processedInput 로직이 완벽하지 않을 수 있으므로 안전장치
                 // 단, 이름 자체에 "]"가 들어가는 경우는 드물다고 가정
                 if (!trimmedPart.contains("[")) {
                     trimmedPart = trimmedPart.substring(0, trimmedPart.length() - 1);
                 }
            }

            authors.add(new ParsedAuthor(trimmedPart, currentRole));
        }

        return authors;
    }
}
