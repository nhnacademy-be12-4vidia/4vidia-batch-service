package com.nhnacademy.book_data_batch.batch.enrichment.aladin.tasklet;

import com.nhnacademy.book_data_batch.batch.dto.BookBatchTarget;
import com.nhnacademy.book_data_batch.batch.enrichment.aladin.client.AladinApiClient;
import com.nhnacademy.book_data_batch.batch.enrichment.aladin.client.AladinQuotaTracker;
import com.nhnacademy.book_data_batch.batch.enrichment.aladin.dto.EnrichmentSuccessDto;
import com.nhnacademy.book_data_batch.batch.enrichment.aladin.dto.api.AladinBookInfoDto;
import com.nhnacademy.book_data_batch.batch.enrichment.aladin.dto.api.AladinItemDto;
import com.nhnacademy.book_data_batch.batch.enrichment.aladin.mapper.AladinDataMapper;
import com.nhnacademy.book_data_batch.domain.enums.BatchStatus;
import com.nhnacademy.book_data_batch.infrastructure.repository.*;
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

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AladinEnrichmentTaskletTest {

    @Mock private BatchRepository batchRepository;
    @Mock private AuthorRepository authorRepository;
    @Mock private BookAuthorRepository bookAuthorRepository;
    @Mock private TagRepository tagRepository;
    @Mock private BookTagRepository bookTagRepository;
    @Mock private BookRepository bookRepository;
    @Mock private BookImageRepository bookImageRepository;
    @Mock private AladinQuotaTracker aladinQuotaTracker;
    @Mock private AladinApiClient aladinApiClient;
    @Mock private AladinDataMapper aladinDataMapper;
    @Mock private StepContribution stepContribution;
    @Mock private ChunkContext chunkContext;

    @Captor private ArgumentCaptor<List<Long>> bookIdsCaptor;

    private AladinEnrichmentTasklet tasklet;
    private List<String> apiKeys;

    @BeforeEach
    void setUp() {
        apiKeys = List.of("test-api-key-1");
        tasklet = new AladinEnrichmentTasklet(
                batchRepository,
                authorRepository,
                bookAuthorRepository,
                tagRepository,
                bookTagRepository,
                bookRepository,
                bookImageRepository,
                aladinQuotaTracker,
                aladinApiClient,
                aladinDataMapper,
                apiKeys
        );
    }

    @Test
    @DisplayName("처리할 도서가 없으면 바로 FINISHED 반환")
    void execute_noPendingBooks_returnsFinished() throws Exception {
        // given
        given(batchRepository.findPendingEnrichmentStatusBook()).willReturn(List.of());

        // when
        RepeatStatus result = tasklet.execute(stepContribution, chunkContext);

        // then
        assertThat(result).isEqualTo(RepeatStatus.FINISHED);
        verify(aladinApiClient, never()).lookupByIsbn(any(), any());
    }

    @Test
    @DisplayName("API 호출 성공 시 도서 정보가 보강됨")
    void execute_apiSuccess_enrichesBook() throws Exception {
        // given
        BookBatchTarget target = new BookBatchTarget(1L, "9788956746425", 100L);
        given(batchRepository.findPendingEnrichmentStatusBook()).willReturn(List.of(target));
        given(aladinQuotaTracker.tryAcquire(anyString())).willReturn(true);

        AladinItemDto mockApiResponse = createMockAladinItem();
        given(aladinApiClient.lookupByIsbn(eq("9788956746425"), anyString()))
                .willReturn(Optional.of(mockApiResponse));

        EnrichmentSuccessDto mockMappedData = createMockEnrichmentSuccess(target);
        given(aladinDataMapper.map(eq(target), eq(mockApiResponse)))
                .willReturn(mockMappedData);

        given(authorRepository.findIdsByNames(anySet())).willReturn(java.util.Map.of("홍길동", 1L));
        given(tagRepository.findIdsByNames(anySet())).willReturn(java.util.Map.of("소설", 10L));

        // when
        RepeatStatus result = tasklet.execute(stepContribution, chunkContext);

        // then
        assertThat(result).isEqualTo(RepeatStatus.FINISHED);
        
        // API 호출 검증
        verify(aladinApiClient).lookupByIsbn("9788956746425", "test-api-key-1");
        
        // 저장 검증
        verify(authorRepository).bulkInsert(anySet());
        verify(tagRepository).bulkInsert(anySet());
        verify(bookRepository).bulkUpdateFromEnrichment(anyList());
        verify(bookImageRepository).bulkInsert(anyList());
        
        // 상태 업데이트 검증
        verify(batchRepository).bulkUpdateEnrichmentStatus(bookIdsCaptor.capture(), eq(BatchStatus.COMPLETED));
        assertThat(bookIdsCaptor.getValue()).containsExactly(1L);
    }

    @Test
    @DisplayName("API 응답이 없으면 FAILED 상태로 업데이트")
    void execute_apiReturnsEmpty_marksFailed() throws Exception {
        // given
        BookBatchTarget target = new BookBatchTarget(1L, "9788956746425", 100L);
        given(batchRepository.findPendingEnrichmentStatusBook()).willReturn(List.of(target));
        given(aladinQuotaTracker.tryAcquire(anyString())).willReturn(true);
        given(aladinApiClient.lookupByIsbn(anyString(), anyString())).willReturn(Optional.empty());

        // when
        RepeatStatus result = tasklet.execute(stepContribution, chunkContext);

        // then
        assertThat(result).isEqualTo(RepeatStatus.FINISHED);
        verify(batchRepository).bulkUpdateEnrichmentFailed(anyList());
        verify(bookRepository, never()).bulkUpdateFromEnrichment(anyList());
    }

    @Test
    @DisplayName("쿼터 초과 시 해당 파티션 처리 중단")
    void execute_quotaExceeded_stopsProcessing() throws Exception {
        // given
        BookBatchTarget target1 = new BookBatchTarget(1L, "9788956746425", 100L);
        BookBatchTarget target2 = new BookBatchTarget(2L, "9788956746426", 101L);
        given(batchRepository.findPendingEnrichmentStatusBook()).willReturn(List.of(target1, target2));
        
        // 첫 번째 호출만 허용
        given(aladinQuotaTracker.tryAcquire(anyString()))
                .willReturn(true)
                .willReturn(false);

        AladinItemDto mockApiResponse = createMockAladinItem();
        given(aladinApiClient.lookupByIsbn(eq("9788956746425"), anyString()))
                .willReturn(Optional.of(mockApiResponse));

        EnrichmentSuccessDto mockMappedData = createMockEnrichmentSuccess(target1);
        given(aladinDataMapper.map(eq(target1), eq(mockApiResponse)))
                .willReturn(mockMappedData);

        given(authorRepository.findIdsByNames(anySet())).willReturn(java.util.Map.of("홍길동", 1L));
        given(tagRepository.findIdsByNames(anySet())).willReturn(java.util.Map.of("소설", 10L));

        // when
        RepeatStatus result = tasklet.execute(stepContribution, chunkContext);

        // then
        assertThat(result).isEqualTo(RepeatStatus.FINISHED);
        // 첫 번째 책만 API 호출됨
        verify(aladinApiClient, times(1)).lookupByIsbn(anyString(), anyString());
    }

    @Test
    @DisplayName("API 예외 발생 시 해당 도서만 FAILED 처리")
    void execute_apiException_marksOnlyFailedBook() throws Exception {
        // given
        BookBatchTarget target = new BookBatchTarget(1L, "9788956746425", 100L);
        given(batchRepository.findPendingEnrichmentStatusBook()).willReturn(List.of(target));
        given(aladinQuotaTracker.tryAcquire(anyString())).willReturn(true);
        given(aladinApiClient.lookupByIsbn(anyString(), anyString()))
                .willThrow(new RuntimeException("API 호출 실패"));

        // when
        RepeatStatus result = tasklet.execute(stepContribution, chunkContext);

        // then
        assertThat(result).isEqualTo(RepeatStatus.FINISHED);
        verify(batchRepository).bulkUpdateEnrichmentFailed(anyList());
    }

    // ===== Helper Methods =====

    private AladinItemDto createMockAladinItem() {
        return new AladinItemDto(
                "테스트 책 제목",
                "홍길동 (지은이)",
                "2024-01-15",
                "이것은 테스트 도서 설명입니다.",
                18000,
                "https://image.aladin.co.kr/cover.jpg",
                "국내도서>소설>한국소설",
                "테스트출판사",
                new AladinBookInfoDto(
                        "부제목입니다",
                        320,
                        "1장. 서론\n2장. 본론\n3장. 결론",
                        List.of(new AladinBookInfoDto.AladinBookAuthorDto("홍길동", "지은이"))
                )
        );
    }

    private EnrichmentSuccessDto createMockEnrichmentSuccess(BookBatchTarget target) {
        return new EnrichmentSuccessDto(
                target.bookId(),
                target.batchId(),
                "이것은 테스트 도서 설명입니다.",
                18000,
                LocalDate.of(2024, 1, 15),
                "부제목입니다",
                320,
                "1장. 서론\n2장. 본론\n3장. 결론",
                List.of(new EnrichmentSuccessDto.AuthorWithRole("홍길동", "지은이")),
                List.of("소설", "한국소설"),
                "https://image.aladin.co.kr/cover.jpg",
                "ko"
        );
    }
}
