package com.nhnacademy.book_data_batch.batch.enrichment.aladin.extractor;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Aladin categoryName에서 태그 추출
 * - "국내도서>건강/취미>건강정보>건강에세이"
 * → ["국내도서", "건강/취미", "건강정보", "건강에세이"]
 */
@Component
public class CategoryTagExtractor {

    private static final String CATEGORY_DELIMITER = ">";

    public List<String> extract(String categoryName) {
        if (!StringUtils.hasText(categoryName)) {
            return Collections.emptyList();
        }

        return Arrays.stream(categoryName.split(CATEGORY_DELIMITER))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .distinct()
                .toList();
    }
}
