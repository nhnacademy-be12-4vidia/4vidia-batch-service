package com.nhnacademy.book_data_batch.parser.impl;

import com.opencsv.CSVParser;
import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.nhnacademy.book_data_batch.dto.ParsingDto;
import com.nhnacademy.book_data_batch.parser.DataParser;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class CsvBookParser implements DataParser {

    @Override
    public String getFileType() {
        return ".csv";
    }

    @Override
    public List<ParsingDto> parsing(File file) throws IOException {
        List<ParsingDto> records = new ArrayList<>();
        CSVParser parser = new CSVParserBuilder()
            .withSeparator(',')
            .withQuoteChar('"')
            .build();

        try (
            FileReader fileReader = new FileReader(file, StandardCharsets.UTF_8);
            CSVReader csvReader = new CSVReaderBuilder(fileReader)
                .withSkipLines(1)
                .withCSVParser(parser)
                .build();
        ) {
            for (String[] data : csvReader) {
                if (data == null || data.length < 18) {
                    continue;
                }
                ParsingDto dto = new ParsingDto();
                dto.setIsbn(data[1]);
                dto.setTitle(data[3]);
                String authorField = data[4];
                dto.setAuthors(parseAuthors(authorField));
                dto.setPublisher(data[5]);
                String stringPrice = data[8];
                if (StringUtils.hasText(stringPrice)) {
                    Double price = Double.parseDouble(stringPrice);
                    dto.setPriceStandard(price.intValue());
                } else {
                    dto.setPriceStandard(null);
                }
                dto.setImageUrl(data[9]);
                dto.setDescription(data[10]);
                dto.setPublishedDate(data[14]);

                records.add(dto);
            }
        }
        return records;
    }

    private List<String> parseAuthors(String authorField) {
        List<String> authorNames = new ArrayList<>();
        if (authorField == null || authorField.isBlank()) {
            return authorNames;
        }
        String[] roleGroups = authorField.split("\\), ");

        for (String group : roleGroups) {
            if (group.contains("(지은이)")) {
                int roleStartIndex = group.lastIndexOf('(');
                if (roleStartIndex == -1) {
                    continue;
                }

                String namesPart = group.substring(0, roleStartIndex).trim();

                String[] names = namesPart.split(",");

                for (String n : names) {
                    String name = n.trim();
                    if (!name.isEmpty()) {
                        authorNames.add(name);
                    }
                }
            }
        }
        return authorNames;
    }

}
