package com.nhnacademy.book_data_batch.service.author.parser;

import com.nhnacademy.book_data_batch.service.author.AuthorParsingService;
import com.nhnacademy.book_data_batch.service.author.dto.ParseResult;
import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 작가 파싱 커버리지 분석 테스트
 */
@Slf4j
@SpringBootTest
class AuthorParserAnalysisTest {

    @Autowired
    private AuthorParsingService parsingService;

    @Test
    @DisplayName("통합 파싱 커버리지 분석")
    void analyzeParsingCoverage() throws IOException, CsvException {
        List<String> testData = loadTestData();
        if (testData.isEmpty()) {
            log.warn("테스트 데이터 없음");
            return;
        }

        int success = 0;
        Map<String, Integer> parserUsage = new HashMap<>();
        Map<String, Integer> roleFreq = new HashMap<>();
        List<String> failed = new ArrayList<>();

        for (String author : testData) {
            ParseResult result = parsingService.parse(author);
            if (result.success()) {
                success++;
                parserUsage.merge(result.strategyName(), 1, Integer::sum);
                result.authors().forEach(a -> roleFreq.merge(a.role(), 1, Integer::sum));
            } else if (failed.size() < 30) {
                failed.add(author);
            }
        }

        double rate = (double) success / testData.size() * 100;
        log.info("=== 파싱 결과 ===");
        log.info("전체: {}건, 성공: {}건 ({}%)", testData.size(), success, String.format("%.2f", rate));

        log.info("--- 파서별 처리 (통합 기준 / 개별 기준) ---");
        for (AuthorRoleParser parser : parsingService.getParsers()) {
            int used = parserUsage.getOrDefault(parser.name(), 0);
            long potential = testData.stream().filter(s -> parser.parse(s).success()).count();
            log.info("  [{}] {}: {}건 / {}건 ({}%)", 
                    parser.priority(), parser.name(), used, potential,
                    String.format("%.2f", (double) potential / testData.size() * 100));
        }

        log.info("--- 역할별 (상위 10개) ---");
        roleFreq.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(10)
                .forEach(e -> log.info("  {}: {}건", e.getKey(), e.getValue()));

        log.info("--- 미처리 ({}) ---", failed.size());
        long noRole = failed.stream()
                .filter(s -> !s.contains("(") && !s.contains(":") && !s.contains(";"))
                .count();
        log.info("  역할 없음: {}건, 기타: {}건", noRole, failed.size() - noRole);
        failed.stream().limit(10).forEach(s -> log.info("  - {}", s));

        assertThat(rate).as("커버리지율").isGreaterThan(70.0);
    }

    private List<String> loadTestData() throws IOException, CsvException {
        List<String> data = new ArrayList<>();
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("test_author_names.csv")) {
            if (is == null) return data;
            try (CSVReader reader = new CSVReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                reader.skip(1); // 헤더
                for (String[] row : reader.readAll()) {
                    if (row.length > 0 && !row[0].isBlank()) {
                        data.add(row[0]);
                    }
                }
            }
        }
        return data;
    }
}
