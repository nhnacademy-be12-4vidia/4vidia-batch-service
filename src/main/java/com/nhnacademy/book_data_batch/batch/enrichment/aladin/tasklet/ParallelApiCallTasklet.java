package com.nhnacademy.book_data_batch.batch.enrichment.aladin.tasklet;

import com.nhnacademy.book_data_batch.batch.enrichment.aladin.client.AladinApiClient;
import com.nhnacademy.book_data_batch.batch.enrichment.aladin.dto.AladinEnrichmentData;
import com.nhnacademy.book_data_batch.batch.enrichment.aladin.dto.AladinItemDto;
import com.nhnacademy.book_data_batch.batch.enrichment.aladin.dto.BookEnrichmentTarget;
import com.nhnacademy.book_data_batch.batch.enrichment.aladin.mapper.AladinDataMapper;
import com.nhnacademy.book_data_batch.batch.enrichment.common.EnrichmentCache;
import com.nhnacademy.book_data_batch.batch.enrichment.common.QuotaTracker;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Step 2: 병렬 Aladin API 호출
 */
@Slf4j
public class ParallelApiCallTasklet implements Tasklet {

    private final EnrichmentCache cache;
    private final AladinApiClient apiClient;
    private final AladinDataMapper dataMapper;
    private final QuotaTracker quotaTracker;
    private final List<String> apiKeys;

    public ParallelApiCallTasklet(
            EnrichmentCache cache,
            AladinApiClient apiClient,
            AladinDataMapper dataMapper,
            QuotaTracker quotaTracker,
            List<String> apiKeys) {
        this.cache = cache;
        this.apiClient = apiClient;
        this.dataMapper = dataMapper;
        this.quotaTracker = quotaTracker;
        this.apiKeys = apiKeys;
    }

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        List<BookEnrichmentTarget> targets = cache.getPendingTargets();

        if (targets.isEmpty()) {
            return RepeatStatus.FINISHED;
        }

        log.info("[Step2] API 호출 시작 - 대상: {}건, API키: {}개", targets.size(), apiKeys.size());

        // 1. API 키 수만큼 파티션으로 분할
        List<List<BookEnrichmentTarget>> partitions = partition(targets, apiKeys.size());

        // 2. Virtual Threads로 병렬 처리
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<Future<?>> futures = new ArrayList<>();
            
            for (int i = 0; i < partitions.size(); i++) {
                final int partitionIdx = i;
                final List<BookEnrichmentTarget> partition = partitions.get(i);
                final String apiKey = apiKeys.get(i);

                futures.add(executor.submit(() -> processPartition(partition, apiKey, partitionIdx)));
            }

            // 모든 가상 스레드 완료 대기
            for (Future<?> future : futures) {
                try {
                    future.get();
                } catch (Exception e) {
                    log.error("[Step2] 파티션 처리 중 오류: {}", e.getMessage());
                }
            }
        }  // try-with-resources로 자동 shutdown

        // 결과 요약 로그
        log.info("[Step2] API 호출 완료 - 성공: {}, 실패: {}", cache.getSuccessCount(), cache.getFailedCount());
        quotaTracker.logUsage();

        return RepeatStatus.FINISHED;
    }

    /**
     * 파티션 처리 (각 가상 스레드에서 실행)
     */
    private void processPartition(
            List<BookEnrichmentTarget> targets,
            String apiKey,
            int partitionIdx) {

        int success = 0;
        int failed = 0;

        for (BookEnrichmentTarget target : targets) {
            // 쿼터 체크
            if (!quotaTracker.canUse(apiKey)) {
                log.debug("[Step2] 파티션 {} 쿼터 소진", partitionIdx);
                break;
            }

            // ISBN 체크 (API 호출 없이 실패 처리 - 쿼터 소모 안 함)
            if (!StringUtils.hasText(target.isbn13())) {
                cache.addFailure(target.bookId(), target.batchId(), "ISBN13 없음");
                failed++;
                continue;
            }

            // API 호출 시도 → 무조건 쿼터 증가 (성공/실패 무관)
            quotaTracker.incrementAndGet(apiKey);

            try {
                Optional<AladinItemDto> response = apiClient.lookupByIsbn(target.isbn13(), apiKey);

                if (response.isPresent()) {
                    AladinEnrichmentData data = dataMapper.map(target, response.get());
                    cache.addSuccess(data);
                    success++;
                } else {
                    cache.addFailure(target.bookId(), target.batchId(), "API 응답 없음");
                    failed++;
                }
            } catch (Exception e) {
                cache.addFailure(target.bookId(), target.batchId(), e.getMessage());
                failed++;
            }
        }

        log.debug("[Step2] 파티션 {} 완료 - 성공: {}, 실패: {}", partitionIdx, success, failed);
    }

    /**
     * 리스트를 n개의 파티션으로 균등 분할
     */
    private <T> List<List<T>> partition(List<T> list, int n) {
        List<List<T>> partitions = new ArrayList<>();
        int size = list.size();
        int partitionSize = (size + n - 1) / n;  // 올림 나눗셈

        for (int i = 0; i < n; i++) {
            int start = i * partitionSize;
            int end = Math.min(start + partitionSize, size);

            if (start < size) {
                partitions.add(new ArrayList<>(list.subList(start, end)));
            } else {
                partitions.add(new ArrayList<>());  // 빈 파티션
            }
        }

        return partitions;
    }
}
