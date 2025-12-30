package com.nhnacademy.book_data_batch.jobs.aladin.writer;

import com.nhnacademy.book_data_batch.jobs.aladin.dto.AladinFetchWrapper;
import com.nhnacademy.book_data_batch.domain.entity.Batch;
import com.nhnacademy.book_data_batch.domain.entity.Book;
import com.nhnacademy.book_data_batch.domain.entity.Publisher;
import com.nhnacademy.book_data_batch.domain.repository.BatchRepository;
import com.nhnacademy.book_data_batch.domain.repository.BookRepository;
import com.nhnacademy.book_data_batch.domain.repository.PublisherRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class AladinFetchWriter implements ItemWriter<AladinFetchWrapper> {

    private final BookRepository bookRepository;
    private final BatchRepository batchRepository;
    private final PublisherRepository publisherRepository;
    private final EntityManager entityManager;

    @Override
    @Transactional
    public void write(Chunk<? extends AladinFetchWrapper> chunk) {
        // 1. Publisher 처리
        Set<String> publisherNames = chunk.getItems().stream()
                .map(AladinFetchWrapper::publisherName)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        if (!publisherNames.isEmpty()) {
            publisherRepository.bulkInsert(publisherNames);
            entityManager.clear();
        }

        // DB에서 ID가 포함된 Publisher 조회
        List<Publisher> existingPublishers = publisherRepository.findByNameIn(publisherNames.stream().toList());
        Map<String, Publisher> publisherMap = existingPublishers.stream()
                .collect(Collectors.toMap(Publisher::getName, p -> p));

        // 2. Book 처리 (Publisher 연결 및 Bulk Insert)
        List<Book> booksToInsert = new ArrayList<>();
        List<String> isbns = new ArrayList<>();

        for (AladinFetchWrapper item : chunk) {
            Book book = item.book();
            String pubName = item.publisherName();

            if (pubName != null && publisherMap.containsKey(pubName)) {
                // Book 객체 재빌드 (Re-build) - Publisher 주입
                book = Book.builder()
                        .isbn(book.getIsbn())
                        .title(book.getTitle())
                        .description(book.getDescription())
                        .publishedDate(book.getPublishedDate())
                        .priceStandard(book.getPriceStandard())
                        .priceSales(book.getPriceSales())
                        .category(book.getCategory())
                        .stock(book.getStock())
                        .publisher(publisherMap.get(pubName))
                        .build();
            }
            booksToInsert.add(book);
            isbns.add(book.getIsbn());
        }

        if (!booksToInsert.isEmpty()) {
            bookRepository.bulkInsert(booksToInsert);
            entityManager.clear();
        }

        // DB에서 ID가 포함된 Book 조회
        List<Book> savedBooks = bookRepository.findAllByIsbnIn(isbns);
        Map<String, Book> bookMap = savedBooks.stream()
                .collect(Collectors.toMap(Book::getIsbn, b -> b));

        // 3. Batch 처리 (Book 연결 및 Bulk Insert)
        List<Batch> batchesToInsert = new ArrayList<>();
        
        for (AladinFetchWrapper item : chunk) {
            String isbn = item.book().getIsbn();
            if (bookMap.containsKey(isbn)) {
                Book savedBook = bookMap.get(isbn);
                Batch batch = Batch.builder()
                        .book(savedBook)
                        .build();
                batchesToInsert.add(batch);
            }
        }

        if (!batchesToInsert.isEmpty()) {
            batchRepository.bulkInsert(batchesToInsert);
        }
    }
}