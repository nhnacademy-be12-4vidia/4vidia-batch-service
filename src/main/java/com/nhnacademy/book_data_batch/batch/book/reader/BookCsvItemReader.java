package com.nhnacademy.book_data_batch.batch.book.reader;

import com.nhnacademy.book_data_batch.batch.book.dto.BookCsvRow;
import java.nio.charset.StandardCharsets;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.mapping.BeanWrapperFieldSetMapper;
import org.springframework.batch.item.file.mapping.DefaultLineMapper;
import org.springframework.batch.item.file.transform.DelimitedLineTokenizer;
import org.springframework.core.io.Resource;

/**
 * BOOK_DB_202112.csv 파일을 한 줄씩 BookCsvRow 로 변환하는 Reader 입니다.
 * <p>
 * - DelimitedLineTokenizer 를 이용해 CSV 헤더 순서를 그대로 매핑합니다.
 * - Spring Batch 의 FlatFileItemReader 를 상속받아 재사용 가능한 Reader 로 구성했습니다.
 */
public class BookCsvItemReader extends FlatFileItemReader<BookCsvRow> {

    public BookCsvItemReader(Resource resource) {
        setName("bookCsvItemReader");
        setResource(resource);
        setEncoding(StandardCharsets.UTF_8.name());
        setLinesToSkip(1);
        setStrict(true);
        setLineMapper(createLineMapper());
    }

    private DefaultLineMapper<BookCsvRow> createLineMapper() {
        DefaultLineMapper<BookCsvRow> lineMapper = new DefaultLineMapper<>();
        lineMapper.setLineTokenizer(createTokenizer());

        BeanWrapperFieldSetMapper<BookCsvRow> fieldSetMapper = new BeanWrapperFieldSetMapper<>();
        fieldSetMapper.setTargetType(BookCsvRow.class);
        lineMapper.setFieldSetMapper(fieldSetMapper);
        return lineMapper;
    }

    private DelimitedLineTokenizer createTokenizer() {
        DelimitedLineTokenizer tokenizer = new DelimitedLineTokenizer();
        tokenizer.setDelimiter(",");
        tokenizer.setQuoteCharacter('"');
        tokenizer.setStrict(false);
        tokenizer.setNames(
            "seqNo",
            "isbnThirteenNo",
            "volumeName",
            "title",
            "authorField",
            "publisher",
            "publishedDate",
            "editionSymbol",
            "price",
            "imageUrl",
            "description",
            "kdcCode",
            "titleSummary",
            "authorSummary",
            "secondaryPublishedDate",
            "internalBookstoreYn",
            "portalSiteYn",
            "isbnTenNo"
        );
        return tokenizer;
    }
}
