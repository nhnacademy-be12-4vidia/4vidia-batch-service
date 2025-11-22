package com.nhnacademy.book_data_batch.service.event;

import com.nhnacademy.book_data_batch.dto.event.BookSavedEvent;
import com.nhnacademy.book_data_batch.client.OllamaClient;
import com.nhnacademy.book_data_batch.document.BookDocument;
import com.nhnacademy.book_data_batch.entity.Book;
import com.nhnacademy.book_data_batch.repository.search.BookSearchRepository;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class BookEventListener {

    private final BookSearchRepository bookSearchRepository;
    private final OllamaClient ollamaClient;

    @Async
//    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @EventListener
    public void handleBookSavedEvent(BookSavedEvent event) {

        log.info("Elasticsearch 인덱싱 시작: {}권", event.getSavedBooks().size());

        List<BookDocument> documents = new ArrayList<>();

        for (Book book : event.getSavedBooks()) {
            String textToEmbed = buildEmbeddingText(book);

            double[] vector = ollamaClient.generateEmbedding(textToEmbed);

            BookDocument doc = BookDocument.builder()
                .id(String.valueOf(book.getId()))
                .title(book.getTitle())
                .isbn(book.getIsbn13())
                .description(book.getDescription())
//                .categories()
                .embedding(vector)
                .build();

            documents.add(doc);
        }

        bookSearchRepository.saveAll(documents);
        log.info("Elasticsearch 인덱싱 완료.");
    }

    private String buildEmbeddingText(Book book) {
        StringBuilder sb = new StringBuilder();

        sb.append("제목: ").append(book.getTitle()).append(" ");

        if (book.getDescription() != null) {
            sb.append("설명: ").append(book.getDescription()).append(" ");
        }
        // 카테고리 추가 후 사용
//        if (book.getCategory() != null) {
//            sb.append("카테고리: ").append(book.getCategory().getname()).append(" ");
//        }
        return sb.toString();
    }

}
