package com.nhnacademy.book_data_batch.batch.enrichment.aladin.tasklet;

import com.nhnacademy.book_data_batch.batch.enrichment.aladin.dto.BookEnrichmentTarget;
import com.nhnacademy.book_data_batch.batch.enrichment.common.EnrichmentCache;
import com.nhnacademy.book_data_batch.batch.enrichment.common.QuotaTracker;
import com.nhnacademy.book_data_batch.repository.BatchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;

import java.util.List;

/**
 * Step 1: PENDING 도서 전체 로드
 * 
 * <p>PENDING 상태의 모든 도서를 메모리에 로드하여 캐시에 저장</p>
 */
@Slf4j
@RequiredArgsConstructor
public class LoadPendingTasklet implements Tasklet {

    private final BatchRepository batchRepository;
    private final EnrichmentCache cache;
    private final QuotaTracker quotaTracker;

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        log.info("[Step1] PENDING 도서 로드 시작");

        // 1. 캐시 및 쿼터 초기화
        cache.clear();
        quotaTracker.reset();

        // 2. PENDING 상태 전체 조회 (Projection)
        List<BookEnrichmentTarget> targets = batchRepository.findAllPending();

        // 3. 캐시에 저장
        cache.setPendingTargets(targets);

        // 4. 처리 건수 로깅 (Tasklet에서는 contribution 사용 제한)
        log.info("[Step1] PENDING 도서 로드 완료: {}건", targets.size());

        return RepeatStatus.FINISHED;
    }
}
