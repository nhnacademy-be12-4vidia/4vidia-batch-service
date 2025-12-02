package com.nhnacademy.book_data_batch.batch.enrichment.aladin.mapper;

import com.nhnacademy.book_data_batch.batch.enrichment.aladin.dto.AladinBookInfoDto;
import com.nhnacademy.book_data_batch.batch.enrichment.aladin.dto.AladinItemDto;
import com.nhnacademy.book_data_batch.entity.Book;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * Aladin API 응답을 Book 엔티티에 매핑하는 Mapper
 * - Aladin API 응답 데이터 → Book 필드 변환
 */
@Slf4j
@Component
public class AladinBookMapper {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /**
     * Aladin 응답으로 Book 필드 업데이트
     *
     * @param book 업데이트할 Book 엔티티
     * @param item Aladin API 응답 아이템
     */
    public void mapToBook(Book book, AladinItemDto item) {
        mapBasicFields(book, item);
        mapBookInfoFields(book, item.bookinfo());
    }

    /**
     * 기본 필드 매핑 (description, price, pubDate)
     */
    private void mapBasicFields(Book book, AladinItemDto item) {
        if (StringUtils.hasText(item.description())) {
            book.setDescription(item.description());
        }
        if (item.priceStandard() != null) {
            book.setPriceStandard(item.priceStandard());
        }
        if (item.priceSales() != null) {
            book.setPriceSales(item.priceSales());
        }
        if (StringUtils.hasText(item.pubDate())) {
            parseAndSetDate(book, item.pubDate());
        }
    }

    /**
     * bookinfo 세부 필드 매핑 (subTitle, itemPage, toc)
     */
    private void mapBookInfoFields(Book book, AladinBookInfoDto bookinfo) {
        if (bookinfo == null) {
            return;
        }
        
        if (StringUtils.hasText(bookinfo.subTitle())) {
            book.setSubtitle(bookinfo.subTitle());
        }
        if (bookinfo.itemPage() != null) {
            book.setPageCount(bookinfo.itemPage());
        }
        if (StringUtils.hasText(bookinfo.toc())) {
            book.setBookIndex(bookinfo.toc());
        }
    }

    private void parseAndSetDate(Book book, String dateStr) {
        try {
            book.setPublishedDate(LocalDate.parse(dateStr, DATE_FORMATTER));
        } catch (DateTimeParseException e) {
            log.debug("날짜 파싱 실패: {}", dateStr);
        }
    }
}
