package com.nhnacademy.book_data_batch.jobs.aladin.writer;

import com.nhnacademy.book_data_batch.jobs.aladin.dto.AladinEnrichmentResult;
import com.nhnacademy.book_data_batch.jobs.aladin.dto.EnrichmentFailureDto;
import com.nhnacademy.book_data_batch.jobs.aladin.dto.EnrichmentSuccessDto;
import com.nhnacademy.book_data_batch.jobs.aladin.mapper.AladinDataMapper;
import com.nhnacademy.book_data_batch.jobs.aladin.writer.persistence.AladinPersistenceService;
import com.nhnacademy.book_data_batch.domain.enums.BatchStatus;
import com.nhnacademy.book_data_batch.domain.repository.BatchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * AladinItemProcessor의 결과(AladinEnrichmentResult)를 받아 DB에 저장하는 Writer.
 * - 성공한 항목: 도서 보강 정보(저자, 태그, 이미지 등) Bulk 저장 + Batch 상태 완료 처리
 * - 실패한 항목: Batch 상태 실패 처리 (재시도 가능 여부에 따라 PENDING 유지 또는 FAILED 처리)
 * - 쿼터 소진 시: 상태만 유지(PENDING)하며 이후 스텝은 정상 진행
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AladinItemWriter implements ItemWriter<AladinEnrichmentResult> {

    private final AladinDataMapper aladinDataMapper;
    private final AladinPersistenceService aladinPersistenceService;
    private final BatchRepository batchRepository;

    @Override
    public void write(Chunk<? extends AladinEnrichmentResult> chunk) throws Exception {
        log.info("[AladinItemWriter] write() 호출됨 - Chunk 크기: {}", chunk.size());

        List<EnrichmentSuccessDto> successDataList = new ArrayList<>();
        List<Long> completedBatchIds = new ArrayList<>();
        List<EnrichmentFailureDto> failedBatches = new ArrayList<>();

        for (AladinEnrichmentResult result : chunk) {
            if (result.isSuccess()) {
                completedBatchIds.add(result.target().batchId());
                log.debug("[AladinItemWriter] Success - Batch ID: {}, hasData: {}",
                        result.target().batchId(), result.itemDto() != null);

                if (result.itemDto() != null) {
                    EnrichmentSuccessDto dto = aladinDataMapper.map(result.target(), result.itemDto());
                    successDataList.add(dto);
                }
            } else {
                String errorMsg = result.errorMessage();
                Long batchId = result.target().batchId();
                log.debug("[AladinItemWriter] Failure - Batch ID: {}, Error: {}", batchId, errorMsg);

                if (result.isRetryable()) {
                    log.debug("[AladinItemWriter] 배치 ID {} 재시도 대기: {}", batchId, errorMsg);
                } else {
                    failedBatches.add(new EnrichmentFailureDto(batchId, errorMsg));
                }
            }
        }

        if (!successDataList.isEmpty()) {
            aladinPersistenceService.saveEnrichmentData(successDataList);
        }

        if (!completedBatchIds.isEmpty()) {
            batchRepository.bulkUpdateEnrichmentStatus(completedBatchIds, BatchStatus.COMPLETED);
            log.info("[AladinItemWriter] {} items marked as COMPLETED", completedBatchIds.size());
        }

        if (!failedBatches.isEmpty()) {
            batchRepository.bulkUpdateEnrichmentFailed(failedBatches);
            log.info("[AladinItemWriter] {} items marked as FAILED", failedBatches.size());
        }
    }
}
