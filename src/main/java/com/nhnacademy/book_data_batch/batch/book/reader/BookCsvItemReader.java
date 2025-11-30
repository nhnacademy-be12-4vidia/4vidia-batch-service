package com.nhnacademy.book_data_batch.batch.book.reader;

import com.nhnacademy.book_data_batch.batch.book.dto.BookCsvRow;
import java.nio.charset.StandardCharsets;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.mapping.DefaultLineMapper;
import org.springframework.batch.item.file.transform.DelimitedLineTokenizer;
import org.springframework.core.io.Resource;

/**
 * BookCsvItemReader
 * - CSV 파일 한 줄 -> BookCsvRow
 * 
 * [처리 흐름]
 * 1. CSV 파일에서 한 줄 읽기
 * 2. DelimitedLineTokenizer로 쉼표(,) 기준 토큰 분리
 * 3. BookCsvFieldSetMapper로 토큰 → BookCsvRow 매핑
 * 4. BookCsvRow 반환
 */
public class BookCsvItemReader extends FlatFileItemReader<BookCsvRow> {

    private static final String[] CSV_COLUMN_NAMES = {
            "seqNo",                    // 순번
            "isbn13",                   // ISBN13
            "volumeNumber",             // 권수
            "title",                    // 제목
            "author",                   // 저자
            "publisher",                // 출판사
            "publishedDate",            // 출판일
            "editionSymbol",            // 부가기호
            "price",                    // 가격
            "imageUrl",                 // 이미지URL
            "description",              // 설명
            "kdcCode",                  // KDC코드
            "titleSearch",              // 제목검색어
            "authorSearch",             // 저자검색어
            "secondaryPublishedDate",   // 부출판일(발행일)
            "internetBookstoreYn",      // 온라인서점여부
            "portalSiteYn",             // 포털사이트여부
            "isbn10"                    // ISBN10
    };

    /**
     * BookCsvItemReader 생성자
     * - setName: Reader 이름 (로그/메타데이터용)
     * - setResource: 읽을 CSV 파일
     * - setEncoding: UTF-8 인코딩
     * - setLinesToSkip: 헤더 1줄 스킵 (파티션에서 추가 조정됨)
     * - setStrict: 파일 없으면 예외 발생
     *  - silent fail: 배치가 실패했는데도 성공으로 처리되는 현상 방지
     * - setLineMapper: 줄 → DTO 변환 로직
     * 
     * @param resource CSV 파일 리소스
     */
    public BookCsvItemReader(Resource resource) {

        setName("bookCsvItemReader");
        setResource(resource);
        setEncoding(StandardCharsets.UTF_8.name());
        setLinesToSkip(1);
        setStrict(true);
        setLineMapper(createLineMapper());
    }

    /**
     * LineMapper 생성
     * - LineTokenizer: 줄을 토큰(필드) 배열로 분리
     * - FieldSetMapper: 토큰 배열을 DTO로 변환
     * 
     * @return 구성된 DefaultLineMapper
     */
    private DefaultLineMapper<BookCsvRow> createLineMapper() {
        DefaultLineMapper<BookCsvRow> lineMapper = new DefaultLineMapper<>();
        lineMapper.setLineTokenizer(createTokenizer());
        lineMapper.setFieldSetMapper(new BookCsvFieldSetMapper());
        return lineMapper;
    }

    /**
     * DelimitedLineTokenizer 생성
     * - delimiter: 쉼표(,)로 필드 구분
     * - quoteCharacter: 큰따옴표(")로 필드 감싸기 (쉼표 포함 필드 처리)
     * - strict: false - 컬럼 수 불일치 허용 (누락된 필드는 빈 문자열)
     * - names: 컬럼 이름 배열 (FieldSetMapper에서 이름으로 접근)
     * 
     * @return 구성된 DelimitedLineTokenizer
     */
    private DelimitedLineTokenizer createTokenizer() {

        DelimitedLineTokenizer tokenizer = new DelimitedLineTokenizer();
        tokenizer.setDelimiter(",");
        tokenizer.setQuoteCharacter('"');
        tokenizer.setStrict(false);
        tokenizer.setNames(CSV_COLUMN_NAMES);
        
        return tokenizer;
    }
}
