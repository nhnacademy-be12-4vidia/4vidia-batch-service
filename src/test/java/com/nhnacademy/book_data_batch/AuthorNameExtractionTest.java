package com.nhnacademy.book_data_batch;

import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.StringUtils;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

class AuthorNameExtractionTest {

    @Test
    void extractUniqueAuthorsToTestResources() throws Exception {
        ClassPathResource resource = new ClassPathResource("data/BOOK_DB_202112.csv");
        Set<String> authorSet = new LinkedHashSet<>();

        try (CSVReader reader = new CSVReaderBuilder(
                new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))
                .withSkipLines(1)
                .withCSVParser(new CSVParserBuilder()
                        .withSeparator(',')
                        .withQuoteChar('"')
                        .build())
                .build()) {

            String[] columns;
            while ((columns = reader.readNext()) != null) {
                String author = columns.length > 4 ? columns[4].trim() : "";
                if (StringUtils.hasText(author)) {
                    authorSet.add(author);
                }
            }
        }

        List<String> sortedAuthors = authorSet.stream()
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();

        Path output = Paths.get("src/test/resources/authors_unique.csv");
        Files.createDirectories(output.getParent());
        Files.write(output, sortedAuthors, StandardCharsets.UTF_8);

        Assertions.assertFalse(sortedAuthors.isEmpty(), "작가 데이터가 추출되지 않았습니다.");
    }
}
