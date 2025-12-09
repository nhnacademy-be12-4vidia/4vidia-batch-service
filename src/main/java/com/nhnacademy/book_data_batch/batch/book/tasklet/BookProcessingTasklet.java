package com.nhnacademy.book_data_batch.batch.book.tasklet;

import com.nhnacademy.book_data_batch.batch.book.cache.InMemoryReferenceDataCache;
import com.nhnacademy.book_data_batch.batch.book.dto.BookCsvRow;
import com.nhnacademy.book_data_batch.batch.book.processor.FieldNormalizer;
import com.nhnacademy.book_data_batch.batch.book.processor.IsbnResolver;
import com.nhnacademy.book_data_batch.domain.Book;
import com.nhnacademy.book_data_batch.domain.Category;
import com.nhnacademy.book_data_batch.domain.Publisher;
import com.nhnacademy.book_data_batch.infrastructure.repository.BookRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Step 2: Book 처리 Tasklet
 * 
 * 1. CSV 데이터 → Book 엔티티 변환 (캐시된 Publisher, Category 사용)
 * 2. Book Bulk INSERT
 * 3. ISBN으로 전체 조회 → Book 캐시 구축
 */
@Slf4j
@RequiredArgsConstructor
public class BookProcessingTasklet implements Tasklet {

    private final InMemoryReferenceDataCache cache;
    private final BookRepository bookRepository;
    private final IsbnResolver isbnResolver;
    private final FieldNormalizer fieldNormalizer;

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {

        List<BookCsvRow> csvRows = cache.getCsvData();
        log.info("[Step2] Book 처리 시작 - CSV {}건", csvRows.size());

        // 1. CSV → Book 변환
        List<Book> books = new ArrayList<>();
        List<String> isbns = new ArrayList<>();

        for (BookCsvRow row : csvRows) {
            Book book = convertToBook(row);
            if (book != null) {
                books.add(book);
                isbns.add(book.getIsbn());
            }
        }

        log.info("[Step2] Book 변환 완료 - {}건 (스킵: {}건)", 
                books.size(), csvRows.size() - books.size());

        // 2. Book Bulk INSERT
        if (!books.isEmpty()) {
            bookRepository.bulkInsert(books);
            log.info("[Step2] Book Bulk INSERT 완료");
        }

        // 3. ISBN으로 Book 캐시 구축
        Set<String> isbnSet = new HashSet<>(isbns);
        cache.buildBookCache(bookRepository, isbnSet);

        contribution.incrementWriteCount(books.size());
        return RepeatStatus.FINISHED;
    }

    /**
     * BookCsvRow → Book 엔티티 변환
     */
    private Book convertToBook(BookCsvRow row) {
        // ISBN 결정
        String isbn = isbnResolver.resolve(row.isbn13(), row.isbn10());
        if (!StringUtils.hasText(isbn)) {
            return null;  // ISBN 없으면 스킵
        }

        // Category 조회
        String kdcCode = fieldNormalizer.normalizeKdc(row.kdcCode());
        Category category = cache.findCategory(kdcCode);

        // Publisher 조회
        Publisher publisher = cache.findPublisher(row.publisher());

        if (row.title() == null || row.title().isBlank()) {
            return null; // 제목 없으면 스킵
        }

        // 필드 정규화
        String description = fieldNormalizer.blankToNull(row.description());
        Integer priceStandard = fieldNormalizer.parsePrice(row.price());
        int priceSales = priceStandard != null ? priceStandard * 9 / 10 : 0;
        Integer volumeNumber = fieldNormalizer.parseVolumeNumber(row.volumeNumber());

        return Book.builder()
                .isbn(isbn)
                .title(row.title())
                .description(description)
                .publisher(publisher)
                .publishedDate(fieldNormalizer.parseDate(row.publishedDate(), row.secondaryPublishedDate()))
                .priceStandard(priceStandard != null ? priceStandard : 0)
                .priceSales(priceSales)
                .stock(10)
                .category(category)
                .volumeNumber(volumeNumber)
                .build();
    }
}
