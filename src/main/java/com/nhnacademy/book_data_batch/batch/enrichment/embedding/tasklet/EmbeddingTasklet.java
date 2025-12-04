package com.nhnacademy.book_data_batch.batch.enrichment.embedding.tasklet;

import com.nhnacademy.book_data_batch.infrastructure.client.OllamaClient;
import com.nhnacademy.book_data_batch.batch.enrichment.embedding.document.BookDocument;
import com.nhnacademy.book_data_batch.batch.enrichment.embedding.dto.EmbeddingFailureDto;
import com.nhnacademy.book_data_batch.batch.enrichment.embedding.dto.EmbeddingSuccessDto;
import com.nhnacademy.book_data_batch.batch.enrichment.dto.BookEmbeddingTarget;
import com.nhnacademy.book_data_batch.domain.enums.BatchStatus;
import com.nhnacademy.book_data_batch.infrastructure.repository.BatchRepository;
import com.nhnacademy.book_data_batch.infrastructure.repository.search.BookSearchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.*;

/**
 * 임베딩 생성 및 Elasticsearch 인덱싱 Tasklet
 * 
 * <p>처리 흐름:</p>
 * <ol>
 *   <li>Aladin 보강 완료(enrichmentStatus=COMPLETED) + 임베딩 미완료(embeddingStatus=PENDING) 도서 조회</li>
 *   <li>Virtual Threads로 병렬 임베딩 생성 (동시 요청 제한)</li>
 *   <li>BookDocument 생성</li>
 *   <li>Elasticsearch Bulk 저장</li>
 *   <li>Batch 상태 업데이트</li>
 * </ol>
 */
@Slf4j
@RequiredArgsConstructor
public class EmbeddingTasklet implements Tasklet {

    private final BatchRepository batchRepository;
    private final BookSearchRepository bookSearchRepository;
    private final OllamaClient ollamaClient;

    /** 동시 요청 제한 (Ollama 서버 부하 방지) */
    private static final int MAX_CONCURRENT_REQUESTS = 32;

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        // 결과 수집용
        ConcurrentLinkedQueue<EmbeddingSuccessDto> successResults = new ConcurrentLinkedQueue<>();
        ConcurrentLinkedQueue<EmbeddingFailureDto> failureResults = new ConcurrentLinkedQueue<>();

        // 1. 임베딩 대상 도서 조회 (Aladin 완료 + 임베딩 미완료)
        List<BookEmbeddingTarget> targets = batchRepository.findPendingEmbeddingStatusBook();
        
        if (targets.isEmpty()) {
            log.debug("[EMBEDDING] 처리할 도서 없음");
            return RepeatStatus.FINISHED;
        }
        
        log.info("[EMBEDDING] 임베딩 대상: {}건", targets.size());

        // 2. 병렬 임베딩 생성 (동시 요청 제한)
        Semaphore semaphore = new Semaphore(MAX_CONCURRENT_REQUESTS);
        
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<CompletableFuture<Void>> futures = targets.stream()
                    .map(target -> CompletableFuture.runAsync(() -> 
                            processEmbedding(target, semaphore, successResults, failureResults), executor))
                    .toList();

            // 모든 임베딩 완료 대기
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        }

        // 3. Elasticsearch Bulk 저장
        if (!successResults.isEmpty()) {
            List<BookDocument> documents = successResults.stream()
                    .map(EmbeddingSuccessDto::document)
                    .filter(Objects::nonNull)
                    .toList();
            
            bookSearchRepository.saveAll(documents);
        }

        log.info("[EMBEDDING] 완료 - 성공: {}, 실패: {}", successResults.size(), failureResults.size());

        // 4. Batch 상태 업데이트
        updateBatchStatus(successResults, failureResults);

        // 5. 처리 건수 기록
        contribution.incrementWriteCount(successResults.size());

        return RepeatStatus.FINISHED;
    }

    /**
     * 단일 도서 임베딩 처리
     */
    private void processEmbedding(
            BookEmbeddingTarget target,
            Semaphore semaphore,
            ConcurrentLinkedQueue<EmbeddingSuccessDto> successResults,
            ConcurrentLinkedQueue<EmbeddingFailureDto> failureResults
    ) {
        try {
            semaphore.acquire();
            
            // 임베딩 텍스트 생성
            String text = target.buildEmbeddingText();
            
            // Ollama API 호출
            double[] embedding = ollamaClient.generateEmbedding(text);
            
            if (embedding != null) {
                BookDocument document = target.toDocument(embedding);
                successResults.add(new EmbeddingSuccessDto(
                        target.bookId(), 
                        target.batchId(), 
                        document
                ));
            } else {
                failureResults.add(new EmbeddingFailureDto(
                        target.bookId(),
                        target.batchId(),
                        "Ollama 응답 없음"
                ));
            }
        } catch (Exception e) {
            log.debug("[EMBEDDING] 실패 - bookId: {}, error: {}", target.bookId(), e.getMessage());
            failureResults.add(new EmbeddingFailureDto(
                    target.bookId(),
                    target.batchId(),
                    e.getMessage()
            ));
        } finally {
            semaphore.release();
        }
    }

    /**
     * Batch 상태 Bulk 업데이트
     */
    private void updateBatchStatus(
            ConcurrentLinkedQueue<EmbeddingSuccessDto> successResults,
            ConcurrentLinkedQueue<EmbeddingFailureDto> failureResults
    ) {
        // 성공: COMPLETED
        if (!successResults.isEmpty()) {
            List<Long> successBatchIds = successResults.stream()
                    .map(EmbeddingSuccessDto::batchId)
                    .toList();
            batchRepository.bulkUpdateEmbeddingStatus(successBatchIds, BatchStatus.COMPLETED);
        }

        // 실패: retryCount 증가 (PENDING 유지)
        if (!failureResults.isEmpty()) {
            List<Object[]> failedData = failureResults.stream()
                    .map(f -> new Object[]{f.batchId(), f.reason()})
                    .toList();
            batchRepository.bulkUpdateEmbeddingFailed(failedData);
        }
    }
}
