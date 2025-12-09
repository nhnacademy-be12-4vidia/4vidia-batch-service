package com.nhnacademy.book_data_batch.batch.enrichment.embedding.tasklet;

import com.nhnacademy.book_data_batch.infrastructure.client.OllamaClient;
import com.nhnacademy.book_data_batch.batch.enrichment.embedding.document.BookDocument;
import com.nhnacademy.book_data_batch.batch.enrichment.embedding.dto.EmbeddingFailureDto;
import com.nhnacademy.book_data_batch.batch.enrichment.embedding.dto.EmbeddingSuccessDto;
import com.nhnacademy.book_data_batch.batch.enrichment.embedding.dto.BookEmbeddingTarget;
import com.nhnacademy.book_data_batch.infrastructure.repository.BatchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Ollama 임베딩 생성 Tasklet
 * Virtual Threads로 병렬로 임베딩을 생성하고 결과를 메모리에 저장
 * 트랜잭션 관리 안 함 (다음 Step에서 DB/ES 저장)
 */
@Slf4j
@RequiredArgsConstructor
public class EmbeddingProcessTasklet implements Tasklet {

    private final BatchRepository batchRepository;
    private final OllamaClient ollamaClient;

    /** 동시 요청 제한 (Ollama 서버 부하 방지) */
    private static final int MAX_CONCURRENT_REQUESTS = 8;

    // 결과 수집용 (정적 저장소 - 다음 Step에서 읽음)
    private static final ConcurrentLinkedQueue<EmbeddingSuccessDto> successResults = new ConcurrentLinkedQueue<>();
    private static final ConcurrentLinkedQueue<EmbeddingFailureDto> failureResults = new ConcurrentLinkedQueue<>();

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        // 결과 큐 초기화
        successResults.clear();
        failureResults.clear();

        // 1. 임베딩 대상 도서 조회 (Aladin 완료 + 임베딩 미완료)
        List<BookEmbeddingTarget> targets = batchRepository.findPendingEmbeddingStatusBook();

        if (targets.isEmpty()) {
            log.debug("[EMBEDDING] 처리할 도서 없음");
            return RepeatStatus.FINISHED;
        }
        
        log.info("[EMBEDDING] 임베딩 대상: {}건", targets.size());

        // 진행 상황 추적용
        AtomicInteger processedCount = new AtomicInteger(0);
        int totalCount = targets.size();
        int logInterval = Math.max(1, totalCount / 100);

        // 2. 병렬 임베딩 생성 (동시 요청 제한, DB 연결 점유 안 함)
        Semaphore semaphore = new Semaphore(MAX_CONCURRENT_REQUESTS);
        
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<CompletableFuture<Void>> futures = targets.stream()
                    .map(target -> CompletableFuture.runAsync(() -> 
                            processEmbedding(target, semaphore, processedCount, totalCount, logInterval), executor))
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
            int logInterval
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

    public static ConcurrentLinkedQueue<EmbeddingSuccessDto> getSuccessResults() {
        return successResults;
    }

    public static ConcurrentLinkedQueue<EmbeddingFailureDto> getFailureResults() {
        return failureResults;
    }
}
