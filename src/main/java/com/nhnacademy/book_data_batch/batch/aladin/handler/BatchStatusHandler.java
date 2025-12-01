package com.nhnacademy.book_data_batch.batch.aladin.handler;

import com.nhnacademy.book_data_batch.batch.aladin.dto.EnrichmentResultDto;
import com.nhnacademy.book_data_batch.entity.enums.BatchStatus;
import com.nhnacademy.book_data_batch.repository.BatchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Batch 상태 업데이트를 담당하는 Handler
 * - 성공: COMPLETED 상태로 업데이트
 * - 실패: FAILED 상태 + 에러 메시지 저장
 */
@Component
@RequiredArgsConstructor
public class BatchStatusHandler implements EnrichmentSaveHandler {

    private final BatchRepository batchRepository;

    @Override
    public void handle(List<EnrichmentResultDto> items) {
        List<EnrichmentResultDto> successItems = items.stream()
                .filter(EnrichmentResultDto::isSuccess)
                .toList();

        List<EnrichmentResultDto> failedItems = items.stream()
                .filter(r -> !r.isSuccess())
                .toList();

        // 성공 항목 상태 업데이트
        if (!successItems.isEmpty()) {
            handleSuccess(successItems);
        }

        // 실패 항목 상태 업데이트
        if (!failedItems.isEmpty()) {
            handleFailure(failedItems);
        }
    }

    @Override
    public int getOrder() {
        return 50;  // 모든 저장 작업 후에 실행
    }

    private void handleSuccess(List<EnrichmentResultDto> items) {
        List<Long> bookIds = items.stream()
                .map(EnrichmentResultDto::bookId)
                .toList();
        
        batchRepository.bulkUpdateEnrichmentStatus(bookIds, BatchStatus.COMPLETED);
    }

    private void handleFailure(List<EnrichmentResultDto> items) {
        List<Object[]> failedData = items.stream()
                .map(r -> new Object[]{r.bookId(), r.errorMessage()})
                .toList();
        
        batchRepository.bulkUpdateEnrichmentFailed(failedData);
    }
}
