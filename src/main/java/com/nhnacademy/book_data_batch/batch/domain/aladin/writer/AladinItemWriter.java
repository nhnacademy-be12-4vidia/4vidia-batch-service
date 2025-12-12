package com.nhnacademy.book_data_batch.batch.domain.aladin.writer;

import com.nhnacademy.book_data_batch.batch.domain.aladin.dto.AladinEnrichmentResult;
import com.nhnacademy.book_data_batch.batch.domain.aladin.dto.EnrichmentSuccessDto;
import com.nhnacademy.book_data_batch.batch.domain.aladin.mapper.AladinDataMapper;
import com.nhnacademy.book_data_batch.batch.domain.aladin.writer.persistence.AladinPersistenceService;
import com.nhnacademy.book_data_batch.domain.enums.BatchStatus;
import com.nhnacademy.book_data_batch.infrastructure.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * AladinItemProcessor의 결과(AladinEnrichmentResult)를 받아 DB에 저장하는 Writer.
 * - 성공한 항목: 도서 보강 정보(저자, 태그, 이미지 등) Bulk 저장 + Batch 상태 완료 처리
 * - 실패한 항목: Batch 상태 실패 처리 (재시도 가능 여부에 따라 PENDING 유지 또는 FAILED 처리)
 * - 쿼터 소진 감지 시: 현재 Chunk까지 저장 후 Step 종료
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AladinItemWriter implements ItemWriter<AladinEnrichmentResult>, StepExecutionListener {

    private final AladinDataMapper aladinDataMapper;
    private final AladinPersistenceService aladinPersistenceService;
    private final BatchRepository batchRepository;
    
    private StepExecution stepExecution;

    @Override
    public void beforeStep(StepExecution stepExecution) {
        this.stepExecution = stepExecution;
    }

    @Override
    public void write(Chunk<? extends AladinEnrichmentResult> chunk) throws Exception {
        log.info("[AladinItemWriter] write() 호출됨 - Chunk 크기: {}", chunk.size());
        
        List<EnrichmentSuccessDto> successDataList = new ArrayList<>();
        List<Long> completedBatchIds = new ArrayList<>();
        List<Object[]> failedBatches = new ArrayList<>(); // [batchId, errorMessage]
        boolean quotaExhausted = false;

        for (AladinEnrichmentResult result : chunk) {
             if (result.isSuccess()) {
                 completedBatchIds.add(result.target().batchId());
                 log.debug("[AladinItemWriter] Success - Batch ID: {}, hasData: {}", 
                     result.target().batchId(), result.itemDto() != null);
                 
                 // 알라딘 데이터가 있는 경우만 저장 로직 수행 (데이터 없음으로 성공한 경우는 상태만 업데이트)
                 if (result.itemDto() != null) {
                     EnrichmentSuccessDto dto = aladinDataMapper.map(result.target(), result.itemDto());
                     successDataList.add(dto);
                 }
             }
             else {
                 // 실패 처리
                 String errorMsg = result.errorMessage();
                 Long batchId = result.target().batchId();
                 log.debug("[AladinItemWriter] Failure - Batch ID: {}, Error: {}", batchId, errorMsg);
                 
                 if ("QUOTA_EXHAUSTED".equals(errorMsg)) {
                     quotaExhausted = true;
                     // 쿼터 소진은 상태 업데이트 하지 않음 (PENDING 유지)
                     log.debug("[AladinItemWriter] 쿼터 소진 감지. Batch ID: {}", batchId);
                 }
                 else if (result.isRetryable()) {
                     // 재시도 가능한 에러 (네트워크 등): PENDING 상태 유지
                     log.debug("[AladinItemWriter] 배치 ID {} 재시도 대기: {}", batchId, errorMsg);
                 } else {
                     // 재시도 불가능한 에러: FAILED 상태로 변경
                     failedBatches.add(new Object[]{batchId, errorMsg});
                 }
             }
         }

        // 1. 보강 데이터 Bulk 저장 (성공한 데이터가 있을 때만)
        if (!successDataList.isEmpty()) {
            aladinPersistenceService.saveEnrichmentData(successDataList);
        }

        // 2. Batch 상태 업데이트
        if (!completedBatchIds.isEmpty()) {
            batchRepository.bulkUpdateEnrichmentStatus(completedBatchIds, BatchStatus.COMPLETED);
            log.info("[AladinItemWriter] {} items marked as COMPLETED", completedBatchIds.size());
        }

        if (!failedBatches.isEmpty()) {
            batchRepository.bulkUpdateEnrichmentFailed(failedBatches);
            log.info("[AladinItemWriter] {} items marked as FAILED", failedBatches.size());
        }
        
        // 3. 쿼터 소진 시 Step 종료 신호 보내기
        if (quotaExhausted) {
            log.warn("[AladinItemWriter] 쿼터 소진으로 인해 Step을 종료합니다.");
            // stepExecution.setTerminateOnly(); // Job 강제 중단 대신 ExitStatus만 설정
            stepExecution.setExitStatus(new ExitStatus("QUOTA_EXHAUSTED")); // 다음 Flow로 이동하기 위한 상태값 설정
        }
    }
}
