package com.nhnacademy.book_data_batch.batch.book.tasklet;

import com.nhnacademy.book_data_batch.batch.book.cache.InMemoryReferenceDataCache;
import com.nhnacademy.book_data_batch.batch.book.dto.BookCsvRow;
import com.nhnacademy.book_data_batch.batch.book.dto.BookImageDto;
import com.nhnacademy.book_data_batch.batch.book.processor.IsbnResolver;
import com.nhnacademy.book_data_batch.entity.Batch;
import com.nhnacademy.book_data_batch.entity.Book;
import com.nhnacademy.book_data_batch.entity.enums.ImageType;
import com.nhnacademy.book_data_batch.repository.BatchRepository;
import com.nhnacademy.book_data_batch.repository.BookImageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Step 3: BookImage + Batch 저장 Tasklet
 * 
 * 1. CSV 데이터에서 이미지 URL 추출
 * 2. Book 캐시에서 Book.id 조회
 * 3. BookImage Bulk INSERT
 * 4. Batch 기록 Bulk INSERT
 */
@Slf4j
@RequiredArgsConstructor
public class BookImageTasklet implements Tasklet {

    private final InMemoryReferenceDataCache cache;
    private final BookImageRepository bookImageRepository;
    private final BatchRepository batchRepository;
    private final IsbnResolver isbnResolver;
    private final String defaultThumbnailUrl;

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {

        List<BookCsvRow> csvRows = cache.getCsvData();
        Collection<Book> savedBooks = cache.getAllBooks();

        log.info("[Step3] BookImage 처리 시작 - CSV {}건, Book 캐시 {}건", 
                csvRows.size(), savedBooks.size());

        // 1. BookImage DTO 생성
        List<BookImageDto> bookImages = new ArrayList<>();

        for (BookCsvRow row : csvRows) {
            String imageUrl = row.imageUrl();
            if (!StringUtils.hasText(imageUrl)) {
                imageUrl = defaultThumbnailUrl;
            }

            // ISBN으로 Book 찾기
            String isbn13 = isbnResolver.resolve(row.isbn13(), row.isbn10());
            if (isbn13 == null) {
                continue;
            }

            Book book = cache.findBook(isbn13);
            if (book == null || book.getId() == null) {
                continue;
            }

            bookImages.add(new BookImageDto(
                    book.getId(),
                    imageUrl.trim(),
                    ImageType.THUMBNAIL.getCode(),
                    0
            ));
        }

        log.info("[Step3] BookImage 변환 완료 - {}건", bookImages.size());

        // 2. BookImage Bulk INSERT
        if (!bookImages.isEmpty()) {
            bookImageRepository.bulkInsert(bookImages);
            log.info("[Step3] BookImage Bulk INSERT 완료");
        }

        // 3. Batch 기록 Bulk INSERT
        List<Batch> batches = savedBooks.stream()
                .map(Batch::new)
                .toList();
        
        if (!batches.isEmpty()) {
            batchRepository.bulkInsert(batches);
            log.info("[Step3] Batch Bulk INSERT 완료 - {}건", batches.size());
        }

        contribution.incrementWriteCount(bookImages.size());
        return RepeatStatus.FINISHED;
    }
}
