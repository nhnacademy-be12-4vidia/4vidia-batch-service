package com.nhnacademy.book_data_batch.batch.aladin.handler;

import com.nhnacademy.book_data_batch.batch.aladin.dto.EnrichmentResultDto;
import com.nhnacademy.book_data_batch.entity.Book;
import com.nhnacademy.book_data_batch.repository.BookRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Book 엔티티 업데이트를 담당하는 Handler
 */
@Component
@RequiredArgsConstructor
public class BookUpdateHandler implements EnrichmentSaveHandler {

    private final BookRepository bookRepository;

    @Override
    public void handle(List<EnrichmentResultDto> items) {
        List<Book> books = items.stream()
                .filter(EnrichmentResultDto::isSuccess)
                .map(EnrichmentResultDto::book)
                .toList();

        if (books.isEmpty()) {
            return;
        }

        bookRepository.bulkUpdateEnrichedFields(books);
    }

    @Override
    public int getOrder() {
        return 10;  // 먼저 실행 (Author/Tag보다 선행)
    }
}
