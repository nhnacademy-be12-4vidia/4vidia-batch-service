package com.nhnacademy.book_data_batch.batch.enrichment.embedding.tasklet;

import com.nhnacademy.book_data_batch.batch.enrichment.dto.BookEmbeddingTarget;
import com.nhnacademy.book_data_batch.batch.enrichment.embedding.document.BookDocument;
import com.nhnacademy.book_data_batch.domain.enums.BatchStatus;
import com.nhnacademy.book_data_batch.infrastructure.client.OllamaClient;
import com.nhnacademy.book_data_batch.infrastructure.repository.BatchRepository;
import com.nhnacademy.book_data_batch.infrastructure.repository.search.BookSearchRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.repeat.RepeatStatus;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmbeddingTaskletTest {

    @Mock private BatchRepository batchRepository;
    @Mock private BookSearchRepository bookSearchRepository;
    @Mock private OllamaClient ollamaClient;
    @Mock private StepContribution stepContribution;
    @Mock private ChunkContext chunkContext;

    @Captor private ArgumentCaptor<List<Long>> batchIdsCaptor;
    @Captor private ArgumentCaptor<List<BookDocument>> documentsCaptor;

    private EmbeddingTasklet tasklet;

    @BeforeEach
    void setUp() {
        tasklet = new EmbeddingTasklet(batchRepository, bookSearchRepository, ollamaClient);
    }

    @Test
    @DisplayName("처리할 도서가 없으면 바로 FINISHED 반환")
    void execute_noPendingBooks_returnsFinished() throws Exception {
        // given
        given(batchRepository.findPendingEmbeddingStatusBook()).willReturn(List.of());

        // when
        RepeatStatus result = tasklet.execute(stepContribution, chunkContext);

        // then
        assertThat(result).isEqualTo(RepeatStatus.FINISHED);
        verify(ollamaClient, never()).generateEmbedding(any());
        verify(bookSearchRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("임베딩 성공 시 Elasticsearch에 저장되고 상태가 COMPLETED로 업데이트됨")
    void execute_embeddingSuccess_savesToElasticsearch() throws Exception {
        // given
        BookEmbeddingTarget target = createMockTarget(1L, 100L);
        given(batchRepository.findPendingEmbeddingStatusBook()).willReturn(List.of(target));

        double[] mockEmbedding = new double[1024];
        given(ollamaClient.generateEmbedding(anyString())).willReturn(mockEmbedding);

        // when
        RepeatStatus result = tasklet.execute(stepContribution, chunkContext);

        // then
        assertThat(result).isEqualTo(RepeatStatus.FINISHED);

        // Ollama 호출 검증
        verify(ollamaClient).generateEmbedding(contains("테스트 책 제목"));

        // Elasticsearch 저장 검증
        verify(bookSearchRepository).saveAll(documentsCaptor.capture());
        List<BookDocument> savedDocs = documentsCaptor.getValue();
        assertThat(savedDocs).hasSize(1);
        assertThat(savedDocs.get(0).getId()).isEqualTo("1");
        assertThat(savedDocs.get(0).getTitle()).isEqualTo("테스트 책 제목");

        // Batch 상태 업데이트 검증
        verify(batchRepository).bulkUpdateEmbeddingStatus(batchIdsCaptor.capture(), eq(BatchStatus.COMPLETED));
        assertThat(batchIdsCaptor.getValue()).containsExactly(100L);
    }

    @Test
    @DisplayName("Ollama 응답이 null이면 FAILED 처리")
    void execute_ollamaReturnsNull_marksFailed() throws Exception {
        // given
        BookEmbeddingTarget target = createMockTarget(1L, 100L);
        given(batchRepository.findPendingEmbeddingStatusBook()).willReturn(List.of(target));
        given(ollamaClient.generateEmbedding(anyString())).willReturn(null);

        // when
        RepeatStatus result = tasklet.execute(stepContribution, chunkContext);

        // then
        assertThat(result).isEqualTo(RepeatStatus.FINISHED);

        // Elasticsearch 저장 안됨
        verify(bookSearchRepository, never()).saveAll(any());

        // 실패 업데이트 검증
        verify(batchRepository).bulkUpdateEmbeddingFailed(anyList());
    }

    @Test
    @DisplayName("Ollama 예외 발생 시 해당 도서만 FAILED 처리")
    void execute_ollamaException_marksFailed() throws Exception {
        // given
        BookEmbeddingTarget target = createMockTarget(1L, 100L);
        given(batchRepository.findPendingEmbeddingStatusBook()).willReturn(List.of(target));
        given(ollamaClient.generateEmbedding(anyString()))
                .willThrow(new RuntimeException("Ollama 연결 실패"));

        // when
        RepeatStatus result = tasklet.execute(stepContribution, chunkContext);

        // then
        assertThat(result).isEqualTo(RepeatStatus.FINISHED);
        verify(batchRepository).bulkUpdateEmbeddingFailed(anyList());
    }

    @Test
    @DisplayName("여러 도서 중 일부만 성공해도 성공한 것만 저장됨")
    void execute_partialSuccess_savesOnlySuccessful() throws Exception {
        // given
        BookEmbeddingTarget target1 = createMockTarget(1L, 100L);
        BookEmbeddingTarget target2 = createMockTarget(2L, 101L);
        given(batchRepository.findPendingEmbeddingStatusBook()).willReturn(List.of(target1, target2));

        double[] mockEmbedding = new double[1024];
        // 첫 번째만 성공
        given(ollamaClient.generateEmbedding(anyString()))
                .willReturn(mockEmbedding)
                .willReturn(null);

        // when
        RepeatStatus result = tasklet.execute(stepContribution, chunkContext);

        // then
        assertThat(result).isEqualTo(RepeatStatus.FINISHED);

        // 성공 1건만 저장
        verify(bookSearchRepository).saveAll(documentsCaptor.capture());
        assertThat(documentsCaptor.getValue()).hasSize(1);

        // 성공/실패 모두 업데이트
        verify(batchRepository).bulkUpdateEmbeddingStatus(anyList(), eq(BatchStatus.COMPLETED));
        verify(batchRepository).bulkUpdateEmbeddingFailed(anyList());
    }

    @Test
    @DisplayName("병렬 처리 시 동시성 제어가 적용됨")
    void execute_concurrentProcessing_respectsSemaphore() throws Exception {
        // given: 50개 도서 (MAX_CONCURRENT_REQUESTS=32 보다 많음)
        List<BookEmbeddingTarget> targets = java.util.stream.IntStream.range(0, 50)
                .mapToObj(i -> createMockTarget((long) i, (long) (100 + i)))
                .toList();

        given(batchRepository.findPendingEmbeddingStatusBook()).willReturn(targets);

        double[] mockEmbedding = new double[1024];
        given(ollamaClient.generateEmbedding(anyString())).willReturn(mockEmbedding);

        // when
        RepeatStatus result = tasklet.execute(stepContribution, chunkContext);

        // then
        assertThat(result).isEqualTo(RepeatStatus.FINISHED);

        // 모든 도서가 처리됨
        verify(ollamaClient, times(50)).generateEmbedding(anyString());
        verify(bookSearchRepository).saveAll(documentsCaptor.capture());
        assertThat(documentsCaptor.getValue()).hasSize(50);
    }

    // ===== Helper Methods =====

    private BookEmbeddingTarget createMockTarget(Long bookId, Long batchId) {
        return new BookEmbeddingTarget(
                bookId,
                batchId,
                "978895674642" + bookId,
                "테스트 책 제목",
                "이것은 테스트 도서 설명입니다.",
                "테스트출판사",
                16200,
                100,
                "홍길동,김철수",
                "소설,한국소설"
        );
    }
}
