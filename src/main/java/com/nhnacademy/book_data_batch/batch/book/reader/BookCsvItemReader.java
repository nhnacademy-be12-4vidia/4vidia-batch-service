package com.nhnacademy.book_data_batch.batch.book.reader;

import com.nhnacademy.book_data_batch.batch.book.dto.BookCsvRow;
import java.nio.charset.StandardCharsets;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.mapping.BeanWrapperFieldSetMapper;
import org.springframework.batch.item.file.mapping.DefaultLineMapper;
import org.springframework.batch.item.file.transform.DelimitedLineTokenizer;
import org.springframework.core.io.Resource;

// Reader: CSV -> BookCsvRow 객체 변환
public class BookCsvItemReader extends FlatFileItemReader<BookCsvRow> {

    public BookCsvItemReader(Resource resource) {
        setName("bookCsvItemReader");
        setResource(resource);
        setEncoding(StandardCharsets.UTF_8.name());
        setLinesToSkip(1);
        setStrict(true); // 파일이 없으면 예외 발생
        setLineMapper(createLineMapper());
    }

    // CSV 파일의 각 라인을 BookCsvRow 객체로 매핑하는 LineMapper 생성
    private DefaultLineMapper<BookCsvRow> createLineMapper() {
        DefaultLineMapper<BookCsvRow> lineMapper = new DefaultLineMapper<>();
        lineMapper.setLineTokenizer(createTokenizer());

        BeanWrapperFieldSetMapper<BookCsvRow> fieldSetMapper = new BeanWrapperFieldSetMapper<>();
        fieldSetMapper.setTargetType(BookCsvRow.class);
        lineMapper.setFieldSetMapper(fieldSetMapper);
        return lineMapper;
    }

    // CSV 파일의 각 라인을 구분자(,)로 토큰화하는 Tokenizer 생성
    private DelimitedLineTokenizer createTokenizer() {
        DelimitedLineTokenizer tokenizer = new DelimitedLineTokenizer();
        tokenizer.setDelimiter(",");
        tokenizer.setQuoteCharacter('"');
        tokenizer.setStrict(false);
        tokenizer.setNames(
            "seqNo",
            "isbn13",
            "volumeNumber",
            "title",
            "author",
            "publisher",
            "publishedDate",
            "editionSymbol",
            "price",
            "imageUrl",
            "description",
            "kdcCode",
            "titleSearch",
            "authorSearch",
            "secondaryPublishedDate",
            "internalBookstoreYn",
            "portalSiteYn",
            "isbn10"
        );
        return tokenizer;
    }
}
