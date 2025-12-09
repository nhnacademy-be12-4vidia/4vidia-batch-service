package com.nhnacademy.book_data_batch.batch.enrichment.embedding.tasklet;

import com.nhnacademy.book_data_batch.batch.enrichment.embedding.document.BookDocument;
import com.nhnacademy.book_data_batch.batch.enrichment.embedding.dto.EmbeddingFailureDto;
import com.nhnacademy.book_data_batch.batch.enrichment.embedding.dto.EmbeddingSuccessDto;
import com.nhnacademy.book_data_batch.domain.enums.BatchStatus;
import com.nhnacademy.book_data_batch.infrastructure.repository.BatchRepository;
import com.nhnacademy.book_data_batch.infrastructure.repository.search.BookSearchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Elasticsearch 인덱싱 및 Batch 상태 업데이트 Tasklet
 * 이전 Step에서 생성한 임베딩 결과를 ES에 저장하고 DB 상태를 업데이트
 * 트랜잭션으로 관리됨
 */
@Slf4j
@RequiredArgsConstructor
public class EmbeddingIndexTasklet implements Tasklet {

    private final BookSearchRepository bookSearchRepository;
    private final BatchRepository batchRepository;

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        // 이전 Step에서 수집한 결과 조회
        List<EmbeddingSuccessDto> successList = new ArrayList<>(EmbeddingProcessTasklet.getSuccessResults());
        List<EmbeddingFailureDto> failureList = new ArrayList<>(EmbeddingProcessTasklet.getFailureResults());

        if (successList.isEmpty() && failureList.isEmpty()) {
            log.debug("[EMBEDDING INDEX] 저장할 데이터 없음");
            return RepeatStatus.FINISHED;
        }

        // 1. Elasticsearch Bulk 저장
        if (!successList.isEmpty()) {
            List<BookDocument> documents = successList.stream()
                    .map(EmbeddingSuccessDto::document)
                    .filter(Objects::nonNull)
                    .toList();
            
            bookSearchRepository.saveAll(documents);
            log.debug("[EMBEDDING INDEX] Elasticsearch 저장 완료: {}건", documents.size());
        }

        // 2. Batch 상태 업데이트
        updateBatchStatus(successList, failureList);

        // 3. 처리 건수 기록
        contribution.incrementWriteCount(successList.size());
        log.info("[EMBEDDING INDEX] 완료 - 성공: {}, 실패: {}", successList.size(), failureList.size());

        return RepeatStatus.FINISHED;
    }

    /**
     * Batch 상태 Bulk 업데이트
     */
    private void updateBatchStatus(
            List<EmbeddingSuccessDto> successList,
            List<EmbeddingFailureDto> failureList
    ) {
        // 성공: COMPLETED
        if (!successList.isEmpty()) {
            List<Long> successBatchIds = successList.stream()
                    .map(EmbeddingSuccessDto::batchId)
                    .toList();
            batchRepository.bulkUpdateEmbeddingStatus(successBatchIds, BatchStatus.COMPLETED);
        }

        // 실패: retryCount 증가 (PENDING 유지)
        if (!failureList.isEmpty()) {
            List<Object[]> failedData = failureList.stream()
                    .map(f -> new Object[]{f.batchId(), f.reason()})
                    .toList();
            batchRepository.bulkUpdateEmbeddingFailed(failedData);
        }
    }
}
