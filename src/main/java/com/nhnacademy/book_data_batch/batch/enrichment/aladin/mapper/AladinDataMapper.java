package com.nhnacademy.book_data_batch.batch.enrichment.aladin.mapper;

import com.nhnacademy.book_data_batch.batch.enrichment.aladin.dto.AladinBookInfoDto;
import com.nhnacademy.book_data_batch.batch.enrichment.aladin.dto.AladinEnrichmentData;
import com.nhnacademy.book_data_batch.batch.enrichment.aladin.dto.AladinEnrichmentData.AuthorWithRole;
import com.nhnacademy.book_data_batch.batch.enrichment.aladin.dto.AladinItemDto;
import com.nhnacademy.book_data_batch.batch.enrichment.aladin.dto.BookEnrichmentTarget;
import com.nhnacademy.book_data_batch.batch.enrichment.aladin.extractor.AuthorExtractor;
import com.nhnacademy.book_data_batch.batch.enrichment.aladin.extractor.CategoryTagExtractor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

/**
 * Aladin API 응답을 AladinEnrichmentData DTO로 변환하는 Mapper
 * 
 * <p>책임:</p>
 * <ul>
 *   <li>API 응답 데이터 → DTO 변환</li>
 *   <li>날짜 파싱</li>
 *   <li>저자/태그 추출 위임</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AladinDataMapper {

    private final AuthorExtractor authorExtractor;
    private final CategoryTagExtractor tagExtractor;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /**
     * BookEnrichmentTarget + AladinItemDto → AladinEnrichmentData 변환
     * 
     * @param target 보강 대상 정보 (bookId, batchId)
     * @param item   Aladin API 응답 아이템
     * @return 변환된 DTO
     */
    public AladinEnrichmentData map(BookEnrichmentTarget target, AladinItemDto item) {
        AladinBookInfoDto bookinfo = item.bookinfo();
        List<AuthorWithRole> authors = authorExtractor.extract(item);
        List<String> tags = tagExtractor.extract(item.categoryName());

        return new AladinEnrichmentData(
                target.bookId(),
                target.batchId(),
                item.description(),
                item.priceStandard(),
                null,  // priceSales - AladinItemDto에 없음 (필요시 추가)
                parseDate(item.pubDate()),
                getSubtitle(bookinfo),
                getPageCount(bookinfo),
                getToc(bookinfo),
                authors,
                tags,
                item.cover()
        );
    }

    /**
     * 날짜 문자열 파싱 (yyyy-MM-dd)
     */
    private LocalDate parseDate(String dateStr) {
        if (!StringUtils.hasText(dateStr)) {
            return null;
        }
        try {
            return LocalDate.parse(dateStr, DATE_FORMATTER);
        } catch (DateTimeParseException e) {
            log.debug("날짜 파싱 실패: {}", dateStr);
            return null;
        }
    }

    /**
     * 부제목 추출
     */
    private String getSubtitle(AladinBookInfoDto bookinfo) {
        if (bookinfo == null) {
            return null;
        }
        return StringUtils.hasText(bookinfo.subTitle()) ? bookinfo.subTitle() : null;
    }

    /**
     * 페이지 수 추출
     */
    private Integer getPageCount(AladinBookInfoDto bookinfo) {
        if (bookinfo == null) {
            return null;
        }
        return bookinfo.itemPage();
    }

    /**
     * 목차 추출
     */
    private String getToc(AladinBookInfoDto bookinfo) {
        if (bookinfo == null) {
            return null;
        }
        return StringUtils.hasText(bookinfo.toc()) ? bookinfo.toc() : null;
    }
}
