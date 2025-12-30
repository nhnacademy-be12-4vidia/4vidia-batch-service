package com.nhnacademy.book_data_batch.jobs.embedding.writer;

import com.nhnacademy.book_data_batch.jobs.embedding.dto.EmbeddingEnrichmentResult;
import com.nhnacademy.book_data_batch.jobs.embedding.document.BookDocument;
import com.nhnacademy.book_data_batch.jobs.embedding.dto.EmbeddingFailureDto;
import com.nhnacademy.book_data_batch.domain.enums.BatchStatus;
import com.nhnacademy.book_data_batch.domain.repository.BatchRepository;
import com.nhnacademy.book_data_batch.domain.repository.search.BookSearchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 임베딩 생성 결과를 Elasticsearch에 저장하고 Batch 상태를 업데이트하는 Writer.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EmbeddingItemWriter implements ItemWriter<EmbeddingEnrichmentResult> {

    private final BookSearchRepository bookSearchRepository;
    private final BatchRepository batchRepository;

    @Override
    public void write(Chunk<? extends EmbeddingEnrichmentResult> chunk) throws Exception {
        List<BookDocument> documentsToIndex = new ArrayList<>();
        List<Long> successBatchIds = new ArrayList<>();
        List<EmbeddingFailureDto> failedBatches = new ArrayList<>();

        for (EmbeddingEnrichmentResult result : chunk) {
            if (result.isSuccess()) {
                if (result.document() != null) {
                    documentsToIndex.add(result.document());
                }
                successBatchIds.add(result.target().batchId());
            } else {
                failedBatches.add(new EmbeddingFailureDto(result.target().batchId(), result.errorMessage()));
            }
        }

        // 1. Elasticsearch Bulk Indexing (문서가 있는 경우만)
        if (!documentsToIndex.isEmpty()) {
            bookSearchRepository.saveAll(documentsToIndex);
            log.debug("[EmbeddingItemWriter] Elasticsearch {}건 인덱싱 완료", documentsToIndex.size());
        }

        // 2. Batch 상태 업데이트
        if (!successBatchIds.isEmpty()) {
            batchRepository.bulkUpdateEmbeddingStatus(successBatchIds, BatchStatus.COMPLETED);
            log.info("[EmbeddingItemWriter] {}건 처리 완료 (COMPLETED)", successBatchIds.size());
        }

        // 3. 실패한 Batch 상태 업데이트
        if (!failedBatches.isEmpty()) {
            batchRepository.bulkUpdateEmbeddingFailed(failedBatches);
            log.info("[EmbeddingItemWriter] {}건 처리 실패 (FAILED)", failedBatches.size());
        }
    }
}
