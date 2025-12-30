package com.nhnacademy.book_data_batch.global.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * <pre>
 * FieldNormalizer
 * - CSV 원시 데이터(BookCsvRow) → 정규화된 데이터(BookNormalizedItem)
 * 
 * [역할]
 * - 문자열 → 타입 변환 (날짜, 숫자 등)
 * - 기본값 처리 (빈 문자열 → null 또는 기본값)
 * - 포맷 정규화 (KDC 코드 등)
 * </pre>
 */
@Slf4j
@Component
public class FieldNormalizer {

    /**
     * 지원하는 날짜 포맷 목록
     */
    private static final List<DateTimeFormatter> DATE_PATTERNS = List.of(
            DateTimeFormatter.ofPattern("yyyy-MM-dd"),  // 2014-05-12
            DateTimeFormatter.ofPattern("yyyyMMdd"),    // 20140512
            DateTimeFormatter.BASIC_ISO_DATE            // 20140512 (ISO)
    );

    /**
     * 문자열을 trim하고, 빈 문자열이면 null 반환
     * @param text 원본 문자열
     * @return trim된 문자열 또는 null
     */
    public String trimOrNull(String text) {
        if (text == null) {
            return null;
        }
        String trimmed = text.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    /**
     * 빈 문자열이면 null 반환
     * @param text 원본 문자열
     * @return 원본 문자열 또는 null
     */
    public String blankToNull(String text) {
        return StringUtils.hasText(text) ? text.trim() : null;
    }

    /**
     * 빈 문자열이면 기본값 반환
     * @param text 원본 문자열
     * @param defaultValue 빈 문자열일 때 반환할 기본값
     * @return 원본 문자열 또는 기본값
     */
    public String defaultIfBlank(String text, String defaultValue) {
        String trimmed = trimOrNull(text);
        return StringUtils.hasText(trimmed) ? trimmed : defaultValue;
    }

    /**
     * 가격 문자열을 Integer로 변환
     * @param priceText 가격 문자열
     * @return 파싱된 가격, 빈 값이면 null, 파싱 실패면 0
     */
    public Integer parsePrice(String priceText) {
        if (!StringUtils.hasText(priceText)) {
            return null;
        }
        try {
            // 쉼표 제거 후 파싱
            String cleanPrice = priceText.trim().replace(",", "");
            return Integer.parseInt(cleanPrice);
        } catch (NumberFormatException ex) {
            log.debug("가격 파싱 실패로 0원 처리: {}", priceText);
            return 0;
        }
    }

    /**
     * 출판일 문자열을 LocalDate로 변환
     * - primary가 있으면 primary 사용
     * - primary가 없으면 secondary 사용 (보조 출판일)
     * @param primary 주 출판일 문자열
     * @param secondary 보조 출판일 문자열 (primary가 없을 때 사용)
     * @return 파싱된 LocalDate, 파싱 실패 시 null
     */
    public LocalDate parseDate(String primary, String secondary) {
        // primary 우선, 없으면 secondary 사용
        String value = StringUtils.hasText(primary) ? primary : secondary;
        if (!StringUtils.hasText(value)) {
            return null;
        }

        String trimmed = value.trim();
        
        // 모든 날짜 포맷 순차 시도
        for (DateTimeFormatter formatter : DATE_PATTERNS) {
            try {
                return LocalDate.parse(trimmed, formatter);
            } catch (Exception ignored) {
                // 다음 포맷 시도 (예외 무시)
            }
        }
        
        // 모든 포맷 실패
        log.debug("날짜 파싱 실패: {}", value);
        return null;
    }

    /**
     * 권수 문자열을 Integer로 변환
     * @param volumeText 권수 문자열
     * @return 파싱된 권수, 실패 시 1
     */
    public Integer parseVolumeNumber(String volumeText) {
        if (!StringUtils.hasText(volumeText)) {
            return 1;  // 기본 권수
        }
        try {
            return Integer.parseInt(volumeText.trim());
        } catch (NumberFormatException ex) {
            return 1;  // 파싱 실패 시 기본 권수
        }
    }

    /**
     * KDC 코드를 정규화합니다.
     * 
     * [KDC(한국십진분류법)]
     * - 도서관에서 도서를 분류하는 데 사용하는 코드
     * - 000: 총류, 100: 철학, 200: 종교, 300: 사회과학, ...
     * - 813.7: 문학(800) > 한국문학(810) > 소설(813) > 장편소설(.7)
     *
     * [정규화 규칙]
     * - 소수점(.) 이전의 숫자만 추출
     * - 숫자가 아닌 문자 제거
     * - 빈 결과는 null 반환
     * @param rawKdc 원본 KDC 코드
     * @return 정규화된 KDC 코드, 유효하지 않으면 null
     */
    public String normalizeKdc(String rawKdc) {
        if (!StringUtils.hasText(rawKdc)) {
            return "UNC";
        }

        String trimmed = rawKdc.trim();
        
        // 소수점(.) 기준으로 분리, 앞부분만 사용
        String[] tokens = trimmed.split("\\.");
        String base = tokens[0];

        if (!StringUtils.hasText(base)) {
            return "UNC";  // 미분류 코드 처리
        }

        // 숫자만 추출 (비숫자 문자 모두 제거)
        String numericOnly = base.replaceAll("\\D", "");
        return StringUtils.hasText(numericOnly) ? numericOnly : "UNC";  // 숫자 없으면 미분류
    }
}
