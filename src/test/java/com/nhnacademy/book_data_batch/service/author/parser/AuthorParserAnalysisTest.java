package com.nhnacademy.book_data_batch.service.author.parser;

import com.nhnacademy.book_data_batch.service.author.parser.dto.ParsedAuthor;
import com.nhnacademy.book_data_batch.service.author.parser.impl.UnifiedAuthorParser;
import com.nhnacademy.book_data_batch.service.author.parser.impl.strategy.*;
import org.junit.jupiter.api.Test;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

class AuthorParserAnalysisTest {

    @Test
    void analyzeParsingRate() throws IOException {
        Path path = Paths.get("src/test/resources/authors_unique.csv");
        if (!Files.exists(path)) {
            return;
        }

        List<String> lines = Files.readAllLines(path);

        // 분석을 위해 전략들을 수동으로 인스턴스화
        List<AuthorParsingStrategy> strategies = List.of(
                new StrictKoreanNameStrategy(),
                new ParenthesizedRoleStrategy(),
                new BracketRoleStrategy(),
                new RoleSuffixStrategy()
        );

        UnifiedAuthorParser parser = new UnifiedAuthorParser(strategies);

        // 전략별 결과 저장 맵
        Map<String, List<String>> successByStrategy = new LinkedHashMap<>();
        for (AuthorParsingStrategy strategy : strategies) {
            successByStrategy.put(strategy.getClass().getSimpleName(), new ArrayList<>());
        }
        successByStrategy.put("UnifiedAuthorParser", new ArrayList<>());

        List<String> failed = new ArrayList<>();

        for (String line : lines) {
            boolean matched = false;
            
            // 세미콜론 분리를 위한 전처리
            if (line.contains(";")) {
                List<ParsedAuthor> result = parser.parse(line);
                if (!result.isEmpty()) {
                    String parsedString = formatResult(result);
                    successByStrategy.get("UnifiedAuthorParser").add(String.format("\"%s\",%s", line.replace("\"", "\"\""), parsedString));
                    matched = true;
                }
            } else {
                String trimmedInput = line.trim();
                for (AuthorParsingStrategy strategy : strategies) {
                    List<ParsedAuthor> result = strategy.parse(trimmedInput);
                    if (!result.isEmpty()) {
                        String parsedString = formatResult(result);
                        successByStrategy.get(strategy.getClass().getSimpleName()).add(String.format("\"%s\",%s", line.replace("\"", "\"\""), parsedString));
                        matched = true;
                        break;
                    }
                }
            }

            if (!matched) {
                failed.add(line);
            }
        }

        // 성공 파일 생성
        for (Map.Entry<String, List<String>> entry : successByStrategy.entrySet()) {
            String strategyName = entry.getKey();
            List<String> results = entry.getValue();
            if (results.isEmpty()) continue;

            Path successPath = Paths.get("src/test/resources/parsing-success-" + strategyName + ".csv");
            try (BufferedWriter writer = Files.newBufferedWriter(successPath, StandardCharsets.UTF_8)) {
                for (String row : results) {
                    writer.write(row);
                    writer.newLine();
                }
            }
        }

        // 실패 파일 생성
        Path failedPath = Paths.get("src/test/resources/parsing-failed.csv");
        try (BufferedWriter writer = Files.newBufferedWriter(failedPath, StandardCharsets.UTF_8)) {
            for (String fail : failed) {
                writer.write(fail);
                writer.newLine();
            }
        }
    }

    private String formatResult(List<ParsedAuthor> parsedList) {
        return parsedList.stream()
                .map(p -> String.format("\"(%s, %s)\"", p.name(), p.role()))
                .collect(Collectors.joining(","));
    }
}

