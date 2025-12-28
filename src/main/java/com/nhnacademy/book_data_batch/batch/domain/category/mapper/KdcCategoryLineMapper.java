package com.nhnacademy.book_data_batch.batch.domain.category.mapper;

import com.nhnacademy.book_data_batch.batch.domain.category.dto.KdcCategoryCsv;
import org.springframework.batch.item.file.LineMapper;
import org.springframework.util.StringUtils;

public class KdcCategoryLineMapper implements LineMapper<KdcCategoryCsv> {

    @Override
    public KdcCategoryCsv mapLine(String line, int lineNumber) {

        // 빈 라인: 빈 코드와 이름 반환
        if (!StringUtils.hasText(line)) {
            return new KdcCategoryCsv("", "");
        }

        // 첫 번째 공백 문자 기준으로 코드와 이름 분리
        String trimmed = line.strip();
        int firstWhitespace = findFirstWhitespace(trimmed);
        if (firstWhitespace == -1) {
            return new KdcCategoryCsv(trimmed, "");
        }

        // 코드와 이름 추출
        String code = trimmed.substring(0, firstWhitespace).trim();
        String name = trimmed.substring(firstWhitespace + 1).trim();
        return new KdcCategoryCsv(code, name);
    }

    // 첫 번째 공백 문자의 인덱스를 찾는 헬퍼 메서드
    private int findFirstWhitespace(String value) {

        return value.chars()
            .mapToObj(c -> (char) c)
            .filter(Character::isWhitespace)
            .map(value::indexOf)
            .findFirst()
            .orElse(-1);
    }
}
