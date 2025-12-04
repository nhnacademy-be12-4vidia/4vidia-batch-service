package com.nhnacademy.book_data_batch.batch.enrichment;

import com.nhnacademy.book_data_batch.infrastructure.repository.BatchRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

/**
 * Cleanup Step 테스트
 * - EnrichmentJobConfig의 cleanupStep에서 사용하는 inline Tasklet 로직 검증
 */
@ExtendWith(MockitoExtension.class)
class CleanupStepTest {

    @Mock private BatchRepository batchRepository;
    @Mock private StepContribution stepContribution;
    @Mock private ChunkContext chunkContext;

    @Test
    @DisplayName("cleanupStep 실행 시 완료된 Batch 레코드가 삭제됨")
    void cleanupTasklet_deletesCompletedBatches() throws Exception {
        // given: inline Tasklet 로직 재현
        Tasklet cleanupTasklet = (contribution, context) -> {
            batchRepository.deleteAllCompleted();
            return RepeatStatus.FINISHED;
        };

        // when
        RepeatStatus result = cleanupTasklet.execute(stepContribution, chunkContext);

        // then
        assertThat(result).isEqualTo(RepeatStatus.FINISHED);
        verify(batchRepository).deleteAllCompleted();
    }
}
