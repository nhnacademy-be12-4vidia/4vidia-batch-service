package com.nhnacademy.book_data_batch.batch.domain.embedding.processor;

import com.nhnacademy.book_data_batch.batch.domain.embedding.document.BookDocument;
import com.nhnacademy.book_data_batch.batch.domain.embedding.dto.BookEmbeddingTarget;
import com.nhnacademy.book_data_batch.batch.domain.embedding.dto.EmbeddingEnrichmentResult;
import com.nhnacademy.book_data_batch.domain.BookAuthor;
import com.nhnacademy.book_data_batch.domain.BookTag;
import com.nhnacademy.book_data_batch.domain.enums.BatchStatus;
import com.nhnacademy.book_data_batch.infrastructure.client.OllamaClient;
import com.nhnacademy.book_data_batch.infrastructure.repository.BatchRepository;
import com.nhnacademy.book_data_batch.infrastructure.repository.BookAuthorRepository;
import com.nhnacademy.book_data_batch.infrastructure.repository.BookTagRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.Semaphore; // Add this import
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class EmbeddingItemProcessor implements ItemProcessor<BookEmbeddingTarget, EmbeddingEnrichmentResult>, StepExecutionListener {

    private static final int MAX_CONCURRENT_OLLAMA_REQUESTS = 8;
    private static final Semaphore OLLAMA_SEMAPHORE = new Semaphore(MAX_CONCURRENT_OLLAMA_REQUESTS);

    private final OllamaClient ollamaClient;
    private final BookAuthorRepository bookAuthorRepository;
    private final BookTagRepository bookTagRepository;
    private final BatchRepository batchRepository; // Count 조회를 위해 주입

    private AtomicInteger processedCount;
    private long totalCount;

    @Override
    public void beforeStep(StepExecution stepExecution) {
        this.processedCount = new AtomicInteger(0);
        this.totalCount = batchRepository.countByEmbeddingStatusAndEnrichmentStatus(
            BatchStatus.PENDING,
            BatchStatus.COMPLETED
        );
        log.info("[EMBEDDING] 처리 시작");
    }

    @Override
    public ExitStatus afterStep(StepExecution stepExecution) {
        return null;
    }

    @Override
    public EmbeddingEnrichmentResult process(BookEmbeddingTarget target) throws Exception {
        try {
            OLLAMA_SEMAPHORE.acquire(); // 세마포어 획득
            
            Long bookId = target.bookId();
            
            // 1. 연관관계 데이터 조회 (Repository 직접 호출)
            List<BookAuthor> bookAuthors = bookAuthorRepository.findByBookId(bookId);
            String authorNames = bookAuthors.stream()
                    .map(bookAuthor -> bookAuthor.getAuthor().getName())
                    .collect(Collectors.joining(", "));
                    
            List<BookTag> bookTags = bookTagRepository.findByBookId(bookId);
            String tagNames = bookTags.stream()
                    .map(bookTag -> bookTag.getTag().getName())
                    .collect(Collectors.joining(", "));

            // 2. 최종 Embedding Target DTO 생성 (새로 조회한 저자/태그 정보로 채움)
            // BookEmbeddingTarget은 불변 객체이므로 새로운 인스턴스를 생성
            BookEmbeddingTarget finalTarget = new BookEmbeddingTarget(
                target.bookId(),
                target.batchId(),
                target.isbn(),
                target.title(),
                target.description(),
                target.publisher(),
                target.priceSales(),
                target.stock(),
                authorNames, // 새로 조회한 값
                tagNames     // 새로 조회한 값
            );

            // 3. 임베딩 텍스트 생성
            String text = finalTarget.buildEmbeddingText();

            // 4. Ollama API 호출
            double[] embedding = ollamaClient.generateEmbedding(text);
            
            // 로깅 로직
            int current = processedCount.incrementAndGet();
            if (totalCount > 0) {
                int logInterval = Math.max(1, (int) (totalCount / 100));
                if (current % logInterval == 0 || current == totalCount) {
                    int percent = (int) ((current * 100.0) / totalCount);
                    log.info("[EMBEDDING] 진행률: {}% ({}/{}) - id: {}", percent, current, totalCount, target.bookId());
                }
            } else { // totalCount가 0이거나 음수인 경우
                 log.info("[EMBEDDING] 진행중: {}건 처리 - id: {}", current, target.bookId());
            }

            if (embedding == null) {
                return new EmbeddingEnrichmentResult(finalTarget, null, false, "Ollama 응답이 null입니다.");
            }

            // 5. BookDocument 생성
            BookDocument document = finalTarget.toDocument(embedding);

            return new EmbeddingEnrichmentResult(finalTarget, document, true, null);

        } catch (Exception e) {
            return new EmbeddingEnrichmentResult(target, null, false, e.getMessage());
        } finally {
            OLLAMA_SEMAPHORE.release(); // 세마포어 반납
        }
    }
}
