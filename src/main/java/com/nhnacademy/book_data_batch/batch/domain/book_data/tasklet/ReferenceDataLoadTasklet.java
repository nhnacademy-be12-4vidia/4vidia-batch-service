package com.nhnacademy.book_data_batch.batch.domain.book_data.tasklet;

import com.nhnacademy.book_data_batch.batch.domain.book_data.cache.InMemoryReferenceDataCache;
import com.nhnacademy.book_data_batch.batch.domain.book_data.dto.BookCsvRow;
import com.nhnacademy.book_data_batch.infrastructure.repository.CategoryRepository;
import com.nhnacademy.book_data_batch.infrastructure.repository.PublisherRepository;
import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.core.io.Resource;
import org.springframework.util.StringUtils;

/**
 * <pre>
 * ReferenceDataLoadTasklet
 * - CSV 전체 메모리 로드 + 참조 데이터 사전 로딩
 *
 * 1. CSV 파일 전체 메모리 로드 → List<BookCsvRow>
 * 2. 출판사 Bulk INSERT (INSERT IGNORE)
 * 3. Publisher 캐시 구축
 * 4. Category 캐시 구축
 * 5. CSV 데이터 캐시 저장
 * </pre>
 */
@RequiredArgsConstructor
public class ReferenceDataLoadTasklet implements Tasklet {

    private final Resource csvResource;
    private final PublisherRepository publisherRepository;
    private final CategoryRepository categoryRepository;
    private final InMemoryReferenceDataCache referenceDataCache;

    // CSV 컬럼 인덱스
    private static final int SEQ_NO = 0;
    private static final int ISBN13 = 1;
    private static final int VOLUME_NUMBER = 2;
    private static final int TITLE = 3;
    private static final int AUTHOR = 4;
    private static final int PUBLISHER = 5;
    private static final int PUBLISHED_DATE = 6;
    private static final int EDITION_SYMBOL = 7;
    private static final int PRICE = 8;
    private static final int IMAGE_URL = 9;
    private static final int DESCRIPTION = 10;
    private static final int KDC_CODE = 11;
    private static final int TITLE_SEARCH = 12;
    private static final int AUTHOR_SEARCH = 13;
    private static final int SECONDARY_PUBLISHED_DATE = 14;
    private static final int INTERNET_BOOKSTORE_YN = 15;
    private static final int PORTAL_SITE_YN = 16;
    private static final int ISBN10 = 17;

    /**
     * Tasklet 실행 메서드
     *
     * 1. CSV 전체 메모리 로드 → List<BookCsvRow>
     * 2. 출판사 Bulk INSERT
     * 3. Publisher 캐시 구축
     * 4. Category 캐시 구축
     * 5. CSV 데이터 캐시 저장
     */
    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {

        // 캐시 초기화
        referenceDataCache.clear();

        // 1. CSV 전체 메모리 로드
        List<BookCsvRow> csvRows = new ArrayList<>();
        Set<String> publisherNames = new HashSet<>();

        try (CSVReader csvReader = new CSVReaderBuilder(
                new InputStreamReader(csvResource.getInputStream(), StandardCharsets.UTF_8))
                .withSkipLines(1)  // 헤더 스킵
                .withCSVParser(new CSVParserBuilder()
                        .withSeparator(',')
                        .withQuoteChar('"')
                        .build())
                .build()) {

            String[] columns;
            while ((columns = csvReader.readNext()) != null) {
                BookCsvRow row = mapToBookCsvRow(columns);
                csvRows.add(row);

                // 출판사 수집
                if (StringUtils.hasText(row.publisher())) {
                    publisherNames.add(row.publisher());
                }
            }
        }

        // 2. 출판사 Bulk INSERT
        publisherRepository.bulkInsert(publisherNames);

        // 3. Publisher 캐시 구축
        referenceDataCache.buildPublisherCache(publisherRepository);

        // 4. Category 캐시 구축
        referenceDataCache.buildCategoryCache(categoryRepository);

        // 5. CSV 데이터 캐시 저장
        referenceDataCache.setCsvData(csvRows);

        // 캐시 준비 완료 표시
        referenceDataCache.markReady();

        // StepContribution에 처리 건수 기록
        contribution.incrementWriteCount(csvRows.size());

        return RepeatStatus.FINISHED;
    }

    /**
     * CSV 컬럼 배열 → BookCsvRow 변환
     */
    private BookCsvRow mapToBookCsvRow(String[] columns) {
        return new BookCsvRow(
                getColumn(columns, SEQ_NO),
                getColumn(columns, ISBN13),
                getColumn(columns, VOLUME_NUMBER),
                getColumn(columns, TITLE),
                getColumn(columns, AUTHOR),
                getColumn(columns, PUBLISHER),
                getColumn(columns, PUBLISHED_DATE),
                getColumn(columns, EDITION_SYMBOL),
                getColumn(columns, PRICE),
                getColumn(columns, IMAGE_URL),
                getColumn(columns, DESCRIPTION),
                getColumn(columns, KDC_CODE),
                getColumn(columns, TITLE_SEARCH),
                getColumn(columns, AUTHOR_SEARCH),
                getColumn(columns, SECONDARY_PUBLISHED_DATE),
                getColumn(columns, INTERNET_BOOKSTORE_YN),
                getColumn(columns, PORTAL_SITE_YN),
                getColumn(columns, ISBN10)
        );
    }

    /**
     * 안전한 컬럼 추출 (인덱스 초과 시 빈 문자열)
     */
    private String getColumn(String[] columns, int index) {
        if (columns == null || index >= columns.length) {
            return "";
        }
        String value = columns[index];
        return value != null ? value.trim() : "";
    }
}
