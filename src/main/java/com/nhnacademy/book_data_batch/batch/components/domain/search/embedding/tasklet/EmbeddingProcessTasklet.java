package com.nhnacademy.book_data_batch.batch.components.domain.search.embedding.tasklet;

import com.nhnacademy.book_data_batch.infrastructure.client.OllamaClient;
import com.nhnacademy.book_data_batch.batch.components.domain.search.embedding.document.BookDocument;
import com.nhnacademy.book_data_batch.batch.components.domain.search.embedding.dto.EmbeddingFailureDto;
import com.nhnacademy.book_data_batch.batch.components.domain.search.embedding.dto.EmbeddingSuccessDto;
import com.nhnacademy.book_data_batch.batch.components.domain.search.embedding.dto.BookEmbeddingTarget;
import com.nhnacademy.book_data_batch.batch.components.core.context.EnrichmentResultsHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Ollama 임베딩 생성 Tasklet
 * Virtual Threads로 병렬로 임베딩을 생성하고 결과를 메모리 저장소에 저장
 * 
 * 트랜잭션 관리 안 함 (다음 Step에서 DB/ES 저장)
 */
@Slf4j
@RequiredArgsConstructor
public class EmbeddingProcessTasklet implements Tasklet {

    private final OllamaClient ollamaClient;
    private final EnrichmentResultsHolder resultsHolder;

    /** 동시 요청 제한 (Ollama 서버 부하 방지) */
    private static final int MAX_CONCURRENT_REQUESTS = 8;

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        // 결과 저장소 획득
        ConcurrentLinkedQueue<EmbeddingSuccessDto> successResults = resultsHolder.getEmbeddingSuccessResults();
        ConcurrentLinkedQueue<EmbeddingFailureDto> failureResults = resultsHolder.getEmbeddingFailureResults();

        // 1. 임베딩 대상 조회 (EmbeddingPrepareTasklet에서 병합된 결과 사용)
        ConcurrentLinkedQueue<BookEmbeddingTarget> targets = resultsHolder.getEmbeddingTargets();
        List<BookEmbeddingTarget> targetList = new ArrayList<>(targets);

        if (targetList.isEmpty()) {
            log.debug("[EMBEDDING] 처리할 도서 없음");
            return RepeatStatus.FINISHED;
        }
        
        log.info("[EMBEDDING] 임베딩 대상: {}건", targetList.size());

        // 진행 상황 추적용
        AtomicInteger processedCount = new AtomicInteger(0);
        int totalCount = targetList.size();
        int logInterval = Math.max(1, totalCount / 100);

        // 2. 병렬 임베딩 생성 (동시 요청 제한, DB 연결 점유 안 함)
        Semaphore semaphore = new Semaphore(MAX_CONCURRENT_REQUESTS);
        
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<CompletableFuture<Void>> futures = targetList.stream()
                    .map(target -> CompletableFuture.runAsync(() -> 
                            processEmbedding(target, semaphore, processedCount, totalCount, logInterval, successResults, failureResults), executor))
                    .toList();

            // 모든 임베딩 완료 대기
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        }

        log.info("[EMBEDDING PROCESS] 완료 - 성공: {}, 실패: {}", successResults.size(), failureResults.size());
        return RepeatStatus.FINISHED;
    }

    /**
     * 단일 도서 임베딩 처리
     */
    private void processEmbedding(
            BookEmbeddingTarget target,
            Semaphore semaphore,
            AtomicInteger processedCount,
            int totalCount,
            int logInterval,
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
            
            // 진행 상황 로깅
            int currentCount = processedCount.incrementAndGet();
            if (currentCount % logInterval == 0 || currentCount == totalCount) {
                int percentage = (int) ((currentCount * 100.0) / totalCount);
                log.info("[EMBEDDING] 진행률: {}% ({}/{})", percentage, currentCount, totalCount);
            }
        }
    }
}
