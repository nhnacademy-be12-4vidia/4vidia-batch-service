package com.nhnacademy.book_data_batch.batch.enrichment.aladin.tasklet;

import com.nhnacademy.book_data_batch.batch.enrichment.aladin.client.AladinApiClient;
import com.nhnacademy.book_data_batch.batch.enrichment.aladin.client.AladinQuotaTracker;
import com.nhnacademy.book_data_batch.batch.enrichment.aladin.dto.*;
import com.nhnacademy.book_data_batch.batch.enrichment.aladin.dto.api.AladinItemDto;
import com.nhnacademy.book_data_batch.batch.enrichment.aladin.exception.RateLimitExceededException;
import com.nhnacademy.book_data_batch.batch.enrichment.aladin.mapper.AladinDataMapper;
import com.nhnacademy.book_data_batch.batch.enrichment.aladin.utils.Partitioner;
import com.nhnacademy.book_data_batch.batch.enrichment.context.EnrichmentResultsHolder;
import com.nhnacademy.book_data_batch.batch.dto.BookBatchTarget;
import com.nhnacademy.book_data_batch.infrastructure.repository.BatchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Aladin API 호출 Tasklet
 * Virtual Threads를 사용하여 병렬로 API를 호출하고 결과를 StepExecutionContext에 저장
 * 트랜잭션 관리 안 함 (다음 Step에서 DB 저장)
 */
@Slf4j
@RequiredArgsConstructor
public class AladinApiTasklet implements Tasklet {

    private final BatchRepository batchRepository;
    private final AladinQuotaTracker aladinQuotaTracker;
    private final AladinApiClient aladinApiClient;
    private final AladinDataMapper aladinDataMapper;
    private final List<String> aladinApiKeys;
    private final EnrichmentResultsHolder resultsHolder;

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {

        // 0. 쿼터 초기화
        aladinQuotaTracker.reset();
        
        ConcurrentLinkedQueue<EnrichmentSuccessDto> successResults = resultsHolder.getAladinSuccessResults();
        ConcurrentLinkedQueue<EnrichmentFailureDto> failedResults = resultsHolder.getAladinFailedResults();

        // 1. PENDING 상태의 도서 조회 (미니 트랜잭션)
        List<BookBatchTarget> pendingTargets = batchRepository.findPendingEnrichmentStatusBook();
        if (pendingTargets.isEmpty()) {
            log.debug("[ALADIN] 처리할 도서 없음");
            return RepeatStatus.FINISHED;
        }

        log.info("[ALADIN] 보강 대상: {}건", pendingTargets.size());

        // 진행 상황 추적용
        AtomicInteger processedCount = new AtomicInteger(0);
        int totalCount = pendingTargets.size();
        int logInterval = Math.max(1, totalCount / 100);

        // 2. API 키 수만큼 파티션으로 분할
        List<List<BookBatchTarget>> partitions = Partitioner.partition(pendingTargets, aladinApiKeys.size());

        // 3. Virtual Threads로 병렬 처리 (DB 연결 점유 안 함)
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<Future<?>> futures = new ArrayList<>();

            for (int i = 0; i < partitions.size(); i++) {
                final int partitionIdx = i;
                final List<BookBatchTarget> partition = partitions.get(i);
                final String apiKey = aladinApiKeys.get(i);

                futures.add(executor.submit(() -> processPartition(partition, apiKey, partitionIdx, processedCount, totalCount, logInterval, successResults, failedResults)));
            }

            // 모든 가상 스레드 완료 대기
            futures.forEach(future -> {
                try {
                    future.get();
                } catch (Exception e) {
                    log.error("[ALADIN] 파티션 처리 중 오류: {}", e.getMessage());
                }
            });
        }

        // 4. 결과 확인
        log.info("[ALADIN API] Step 완료 - 성공: {}, 실패: {}", successResults.size(), failedResults.size());
        return RepeatStatus.FINISHED;
    }

    private void processPartition(
            List<BookBatchTarget> targets,
            String apiKey,
            int partitionIdx,
            AtomicInteger processedCount,
            int totalCount,
            int logInterval,
            ConcurrentLinkedQueue<EnrichmentSuccessDto> successResults,
            ConcurrentLinkedQueue<EnrichmentFailureDto> failedResults
    ) {
        int partitionSuccess = 0;
        int partitionFailed = 0;

        for (BookBatchTarget target : targets) {
            // 쿼터 체크
            if (!aladinQuotaTracker.tryAcquire(apiKey)) {
                break;
            }

            try {
                Optional<AladinItemDto> response = aladinApiClient.lookupByIsbn(target.isbn13(), apiKey);

                if (response.isPresent()) {
                    EnrichmentSuccessDto data = aladinDataMapper.map(target, response.get());
                    successResults.add(data);
                    partitionSuccess++;
                } else {
                    failedResults.add(new EnrichmentFailureDto(target.bookId(), target.batchId(), "API 응답 없음"));
                    partitionFailed++;
                }
            } catch (RateLimitExceededException e) {
                log.warn("[ALADIN] 파티션-{} 쿼터 초과로 중단: {}", partitionIdx, e.getMessage());
                break;
            } catch (Exception e) {
                failedResults.add(new EnrichmentFailureDto(target.bookId(), target.batchId(), e.getMessage()));
                partitionFailed++;
            }

            // 진행 상황 로깅
            int currentCount = processedCount.incrementAndGet();
            if (currentCount % logInterval == 0 || currentCount == totalCount) {
                int percentage = (int) ((currentCount * 100.0) / totalCount);
                log.info("[ALADIN] 진행률: {}% ({}/{})", percentage, currentCount, totalCount);
            }
        }

        log.debug("[ALADIN] 파티션-{} 완료 - 성공: {}, 실패: {}", partitionIdx, partitionSuccess, partitionFailed);
    }
}
