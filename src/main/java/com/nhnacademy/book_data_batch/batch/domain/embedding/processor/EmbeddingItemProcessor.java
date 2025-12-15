package com.nhnacademy.book_data_batch.batch.domain.embedding.processor;

import com.nhnacademy.book_data_batch.batch.domain.embedding.client.OllamaClient;
import com.nhnacademy.book_data_batch.batch.domain.embedding.document.BookDocument;
import com.nhnacademy.book_data_batch.batch.domain.embedding.dto.BookEmbeddingTarget;
import com.nhnacademy.book_data_batch.batch.domain.embedding.dto.EmbeddingEnrichmentResult;
import com.nhnacademy.book_data_batch.domain.Batch;
import com.nhnacademy.book_data_batch.domain.Book;
import com.nhnacademy.book_data_batch.domain.enums.BatchStatus;
import com.nhnacademy.book_data_batch.infrastructure.repository.BatchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors; // Reintroduce

@Slf4j
@Component
@RequiredArgsConstructor
public class EmbeddingItemProcessor implements ItemProcessor<Batch, EmbeddingEnrichmentResult>, StepExecutionListener {

    private static final int MAX_CONCURRENT_OLLAMA_REQUESTS = 8;
    private static final Semaphore OLLAMA_SEMAPHORE = new Semaphore(MAX_CONCURRENT_OLLAMA_REQUESTS);

    private final OllamaClient ollamaClient;
    private final BatchRepository batchRepository;

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
    public EmbeddingEnrichmentResult process(Batch batch) throws Exception {
        BookEmbeddingTarget target = null; // Declare outside try-block to be accessible in catch
        try {
            OLLAMA_SEMAPHORE.acquire(); // 세마포어 획득
            
            Book book = batch.getBook();
            
            // 1. 연관관계 데이터 조회 (객체 그래프 탐색 - hibernate.default_batch_fetch_size로 최적화됨)
            String authorNames = book.getBookAuthors().stream()
                    .map(bookAuthor -> bookAuthor.getAuthor().getName())
                    .collect(Collectors.joining(", "));
                    
            String tagNames = book.getBookTags().stream()
                    .map(bookTag -> bookTag.getTag().getName())
                    .collect(Collectors.joining(", "));

            // 2. Embedding Target DTO 생성
            target = new BookEmbeddingTarget(
                book.getId(),
                batch.getId(),
                book.getIsbn(),
                book.getTitle(),
                book.getDescription(),
                book.getPublisher() != null ? book.getPublisher().getName() : "",
                book.getPriceSales(),
                book.getStock(),
                authorNames, 
                tagNames     
            );

            // 3. 임베딩 텍스트 생성
            String text = target.buildEmbeddingText();

            // 4. Ollama API 호출
            double[] embedding = ollamaClient.generateEmbedding(text);
            
            // 로깅 로직
            int current = processedCount.incrementAndGet();
            if (totalCount > 0) {
                int logInterval = Math.max(1, (int) (totalCount / 100));
                if (current % logInterval == 0 || current == totalCount) {
                    int percent = (int) ((current * 100.0) / totalCount);
                    log.info("[EMBEDDING] 진행률: {}% ({}/{}) - id: {}", percent, current, totalCount, book.getId());
                }
            } else { // totalCount가 0이거나 음수인 경우
                 log.info("[EMBEDDING] 진행중: {}건 처리 - id: {}", current, book.getId());
            }

            if (embedding == null) {
                return new EmbeddingEnrichmentResult(target, null, false, "Ollama 응답이 null입니다.");
            }

            // 5. BookDocument 생성
            BookDocument document = target.toDocument(embedding);

            return new EmbeddingEnrichmentResult(target, document, true, null);

        } catch (Exception e) {
            // target이 null이면 최소한의 정보로 생성 (에러 처리를 위해)
            if (target == null) {
                 Book book = batch.getBook(); // Need book to get bookId and isbn
                 target = new BookEmbeddingTarget(
                    book.getId(), batch.getId(), book.getIsbn(), book.getTitle(), 
                    null, null, null, null, null, null
                 );
            }
            return new EmbeddingEnrichmentResult(target, null, false, e.getMessage());
        } finally {
            OLLAMA_SEMAPHORE.release(); // 세마포어 반납
        }
    }
}