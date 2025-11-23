package com.nhnacademy.book_data_batch.batch.book.processor;

import com.nhnacademy.book_data_batch.batch.book.dto.BookCsvRow;
import com.nhnacademy.book_data_batch.batch.book.dto.BookNormalizedItem;
import com.nhnacademy.book_data_batch.batch.book.parser.AuthorRoleParser;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.util.StringUtils;

/**
 * CSV 로부터 읽은 문자열 데이터를 정규화하여 Writer 가 바로 사용할 수 있도록 변환
 */
@Slf4j
public class BookItemProcessor implements ItemProcessor<BookCsvRow, BookNormalizedItem> {

    private static final List<DateTimeFormatter> DATE_FORMATTERS = List.of(
        DateTimeFormatter.ofPattern("yyyy-MM-dd"),
        DateTimeFormatter.ofPattern("yyyyMMdd"),
        DateTimeFormatter.BASIC_ISO_DATE
    );
    private static final String DEFAULT_PUBLISHER = "미상 출판사";
    private static final String DEFAULT_AUTHOR = "작자미상";
    private static final String DEFAULT_ROLE = "지은이";
    private final AuthorRoleParser authorRoleParser = new AuthorRoleParser(DEFAULT_AUTHOR, DEFAULT_ROLE);

    @Override
    public BookNormalizedItem process(BookCsvRow item) {
        String isbn = safeTrim(item.getIsbnThirteenNo());
        String title = safeTrim(item.getTitle());
        if (!StringUtils.hasText(isbn) || !StringUtils.hasText(title)) { // TODO: 제목은 없어도 괜찮지 않나?
            log.warn("필수값 누락으로 레코드를 건너뜁니다. isbn={}", item.getIsbnThirteenNo());
            return null;
        }

        String publisher = StringUtils.hasText(item.getPublisher())
            ? item.getPublisher().trim()
            : DEFAULT_PUBLISHER;

        // TODO: 아... 카테고리를 null로 두고 하는 게 당연한데, 뭔 생각으로 했지
        String kdcCode = normalizeKdc(item.getKdcCode());
        if (!StringUtils.hasText(kdcCode)) {
            log.debug("KDC 코드가 없어 레코드를 건너뜁니다. isbn={}, title={}", isbn, title);
            return null;
        }

        return BookNormalizedItem.builder()
            .isbn13(isbn)
            .title(title)
            .subtitle(blankToNull(item.getTitleSummary()))
            .authorRoles(authorRoleParser.parse(item.getAuthorField()))
            .publisherName(publisher)
            .priceStandard(parsePrice(item.getPrice()))
            .imageUrl(blankToNull(item.getImageUrl()))
            .description(blankToNull(item.getDescription()))
            .publishedDate(parsePublishedDate(item.getPublishedDate(), item.getSecondaryPublishedDate()))
            .kdcCode(kdcCode)
            .volumeNumber(parseVolumeNumber(item.getVolumeName()))
            .build();
    }

    private Integer parsePrice(String priceText) {
        if (!StringUtils.hasText(priceText)) {
            return null;
        }
        try {
            double price = Double.parseDouble(priceText.replaceAll(",", ""));
            return (int) Math.round(price);
        } catch (NumberFormatException ex) {
            log.debug("가격 파싱 실패로 무시합니다. price={}, message={} ", priceText, ex.getMessage());
            return null;
        }
    }

    private LocalDate parsePublishedDate(String primary, String secondary) {
        String value = StringUtils.hasText(primary) ? primary : secondary;
        if (!StringUtils.hasText(value)) {
            return null;
        }
        value = value.trim();
        for (DateTimeFormatter formatter : DATE_FORMATTERS) {
            try {
                return LocalDate.parse(value, formatter);
            } catch (Exception ignored) {
                // 포맷이 맞지 않으면 다음 후보로 넘어간다.
            }
        }
        log.debug("지원하지 않는 날짜 포맷으로 기본값을 사용합니다. value={}", value);
        return null;
    }

    private Integer parseVolumeNumber(String volumeText) {
        if (!StringUtils.hasText(volumeText)) {
            return 1;
        }
        try {
            return Integer.parseInt(volumeText.trim());
        } catch (NumberFormatException ex) {
            return 1;
        }
    }

    private String normalizeKdc(String rawKdc) {
        if (!StringUtils.hasText(rawKdc)) {
            return null;
        }
        String trimmed = rawKdc.trim();
        String[] tokens = trimmed.split("\\.");
        String base = tokens[0];
        if (!StringUtils.hasText(base)) {
            return null;
        }
        base = base.replaceAll("[^0-9]", "");
        if (!StringUtils.hasText(base)) {
            return null;
        }
        int numeric = Integer.parseInt(base);
        return String.format(Locale.KOREA, "%03d", numeric);
    }

    private String safeTrim(String text) {
        return text == null ? null : text.trim();
    }

    private String blankToNull(String text) {
        return StringUtils.hasText(text) ? text.trim() : null;
    }
}
