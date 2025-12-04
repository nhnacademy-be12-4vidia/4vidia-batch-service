package com.nhnacademy.book_data_batch.batch.enrichment.aladin.tasklet;

import com.nhnacademy.book_data_batch.batch.dto.BookImageDto;
import com.nhnacademy.book_data_batch.batch.enrichment.aladin.client.AladinApiClient;
import com.nhnacademy.book_data_batch.batch.enrichment.aladin.client.AladinQuotaTracker;
import com.nhnacademy.book_data_batch.batch.enrichment.aladin.dto.*;
import com.nhnacademy.book_data_batch.batch.enrichment.aladin.dto.api.AladinItemDto;
import com.nhnacademy.book_data_batch.batch.enrichment.aladin.mapper.AladinDataMapper;
import com.nhnacademy.book_data_batch.batch.enrichment.utils.Partitioner;
import com.nhnacademy.book_data_batch.batch.dto.BookBatchTarget;
import com.nhnacademy.book_data_batch.domain.enums.BatchStatus;
import com.nhnacademy.book_data_batch.domain.enums.ImageType;
import com.nhnacademy.book_data_batch.infrastructure.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
public class AladinEnrichmentTasklet implements Tasklet {

    private final BatchRepository batchRepository;
    private final AuthorRepository authorRepository;
    private final BookAuthorRepository bookAuthorRepository;
    private final TagRepository tagRepository;
    private final BookTagRepository bookTagRepository;
    private final BookRepository bookRepository;
    private final BookImageRepository bookImageRepository;

    private final AladinQuotaTracker aladinQuotaTracker;
    private final AladinApiClient aladinApiClient;
    private final AladinDataMapper aladinDataMapper;
    private final List<String> aladinApiKeys;

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        // 결과 수집용
        ConcurrentLinkedQueue<EnrichmentSuccessDto> successResults = new ConcurrentLinkedQueue<>();
        ConcurrentLinkedQueue<EnrichmentFailureDto> failedResults = new ConcurrentLinkedQueue<>();

        // 0. 쿼터 초기화
        aladinQuotaTracker.reset();

        // 1. PENDING 상태의 도서 조회
        List<BookBatchTarget> pendingTargets = batchRepository.findPendingEnrichmentStatusBook();
        if (pendingTargets.isEmpty()) {
            log.debug("[ALADIN] 처리할 도서 없음");
            return RepeatStatus.FINISHED;
        }

        log.info("[ALADIN] 보강 대상: {}건", pendingTargets.size());

        // 진행 상황 추적용
        AtomicInteger processedCount = new AtomicInteger(0);
        int totalCount = pendingTargets.size();
        int logInterval = Math.max(1, totalCount / 10); // 10% 단위로 로깅

        // 2. API 키 수만큼 파티션으로 분할
        List<List<BookBatchTarget>> partitions = Partitioner.partition(pendingTargets, aladinApiKeys.size());

        // 3. Virtual Threads로 병렬 처리
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<Future<?>> futures = new ArrayList<>();

            for (int i = 0; i < partitions.size(); i++) {
                final int partitionIdx = i;
                final List<BookBatchTarget> partition = partitions.get(i);
                final String apiKey = aladinApiKeys.get(i);

                futures.add(executor.submit(() -> processPartition(partition, apiKey, partitionIdx, successResults, failedResults, processedCount, totalCount, logInterval)));
            }

            // 모든 가상 스레드 완료 대기
            futures.forEach(future -> {
                try {
                    future.get();
                } catch (Exception e) {
                    log.error("[ALADIN] 파티션 처리 중 오류: {}", e.getMessage());
                }
            });
        }

        // 4. Bulk 판단
        if (successResults.isEmpty() && failedResults.isEmpty()) {
            log.debug("[ALADIN] 저장할 데이터 없음");
            return RepeatStatus.FINISHED;
        }
        List<EnrichmentSuccessDto> successList = new ArrayList<>(successResults);

        // 5. 처리
        saveAuthors(successList);
        saveTags(successList);
        saveBooks(successList);
        saveImages(successList);
        updateBatchStatus(successList, new ArrayList<>(failedResults));

        // 6. 처리 건수 기록
        contribution.incrementWriteCount(successList.size());
        log.info("[ALADIN] 완료 - 성공: {}, 실패: {}", successList.size(), failedResults.size());

        return RepeatStatus.FINISHED;
    }

    private void processPartition(
            List<BookBatchTarget> targets,
            String apiKey,
            int partitionIdx,
            ConcurrentLinkedQueue<EnrichmentSuccessDto> successResults,
            ConcurrentLinkedQueue<EnrichmentFailureDto> failedResults,
            AtomicInteger processedCount,
            int totalCount,
            int logInterval
    ) {
        int partitionSuccess = 0;
        int partitionFailed = 0;

        for (BookBatchTarget target : targets) {
            // 쿼터 체크
            if (!aladinQuotaTracker.tryAcquire(apiKey)) {
                break;
            }

            try {
                Optional<AladinItemDto> response = aladinApiClient.lookupByIsbn(target.isbn13(), apiKey);

                if (response.isPresent()) {
                    EnrichmentSuccessDto data = aladinDataMapper.map(target, response.get());
                    successResults.add(data);
                    partitionSuccess++;
                } else {
                    failedResults.add(new EnrichmentFailureDto(target.bookId(), target.batchId(), "API 응답 없음"));
                    partitionFailed++;
                }
            } catch (Exception e) {
                failedResults.add(new EnrichmentFailureDto(target.bookId(), target.batchId(), e.getMessage()));
                partitionFailed++;
            }

            // 진행 상황 로깅
            int currentCount = processedCount.incrementAndGet();
            if (currentCount % logInterval == 0 || currentCount == totalCount) {
                int percentage = (int) ((currentCount * 100.0) / totalCount);
                log.info("[ALADIN] 진행률: {}% ({}/{})", percentage, currentCount, totalCount);
            }
        }

        log.debug("[ALADIN] 파티션-{} 완료 - 성공: {}, 실패: {}", partitionIdx, partitionSuccess, partitionFailed);
    }

    /**
     * Author 저장 및 BookAuthor 관계 생성
     */
    private void saveAuthors(List<EnrichmentSuccessDto> results) {
        // 1. 모든 저자 이름 수집 (중복 제거)
        Set<String> authorNames = results.stream()
                .filter(EnrichmentSuccessDto::hasAuthors)
                .flatMap(r -> r.authors().stream())
                .map(EnrichmentSuccessDto.AuthorWithRole::name)
                .collect(Collectors.toSet());

        if (authorNames.isEmpty()) {
            log.debug("[TASKLET] 저장할 Author 없음");
            return;
        }

        // 2. Author bulk insert (INSERT IGNORE)
        authorRepository.bulkInsert(authorNames);

        // 3. Author ID 조회
        Map<String, Long> authorIdMap = authorRepository.findIdsByNames(authorNames);

        // 4. BookAuthor 관계 생성
        List<BookAuthorDto> bookAuthors = new ArrayList<>();
        for (EnrichmentSuccessDto data : results) {
            if (!data.hasAuthors()) continue;

            for (EnrichmentSuccessDto.AuthorWithRole author : data.authors()) {
                Long authorId = authorIdMap.get(author.name());
                if (authorId != null) {
                    bookAuthors.add(new BookAuthorDto(data.bookId(), authorId, author.role()));
                }
            }
        }

        // 5. BookAuthor bulk insert
        if (!bookAuthors.isEmpty()) {
            bookAuthorRepository.bulkInsert(bookAuthors);
        }
    }

    /**
     * Tag 저장 및 BookTag 관계 생성
     */
    private void saveTags(List<EnrichmentSuccessDto> results) {
        // 1. 모든 태그 이름 수집 (중복 제거)
        Set<String> tagNames = results.stream()
                .filter(EnrichmentSuccessDto::hasTags)
                .flatMap(r -> r.tags().stream())
                .collect(Collectors.toSet());

        if (tagNames.isEmpty()) {
            log.debug("[TASKLET] 저장할 Tag 없음");
            return;
        }

        // 2. Tag bulk insert (INSERT IGNORE)
        tagRepository.bulkInsert(tagNames);

        // 3. Tag ID 조회
        Map<String, Long> tagIdMap = tagRepository.findIdsByNames(tagNames);

        // 4. BookTag 관계 생성
        List<long[]> bookTagPairs = new ArrayList<>();
        for (EnrichmentSuccessDto data : results) {
            if (!data.hasTags()) continue;

            for (String tagName : data.tags()) {
                Long tagId = tagIdMap.get(tagName);
                if (tagId != null) {
                    bookTagPairs.add(new long[]{data.bookId(), tagId});
                }
            }
        }

        // 5. BookTag bulk insert
        if (!bookTagPairs.isEmpty()) {
            bookTagRepository.bulkInsert(bookTagPairs);
        }
    }

    /**
     * Book 엔티티 업데이트
     */
    private void saveBooks(List<EnrichmentSuccessDto> results) {
        if (results.isEmpty()) {
            return;
        }

        bookRepository.bulkUpdateFromEnrichment(results);
    }

    /**
     * BookImage 저장
     */
    private void saveImages(List<EnrichmentSuccessDto> results) {
        List<BookImageDto> images = results.stream()
                .filter(EnrichmentSuccessDto::hasCoverUrl)
                .map(r -> new BookImageDto(
                        r.bookId(),
                        r.coverUrl(),
                        ImageType.THUMBNAIL.getCode(),
                        0  // displayOrder
                ))
                .toList();

        if (images.isEmpty()) {
            log.debug("[TASKLET] 저장할 BookImage 없음");
            return;
        }

        bookImageRepository.bulkInsert(images);
    }

    /**
     * Batch 상태 업데이트
     */
    private void updateBatchStatus(List<EnrichmentSuccessDto> success, List<EnrichmentFailureDto> failed) {
        // 성공 항목: COMPLETED
        if (!success.isEmpty()) {
            List<Long> successBookIds = success.stream()
                    .map(EnrichmentSuccessDto::bookId)
                    .toList();
            batchRepository.bulkUpdateEnrichmentStatus(successBookIds, BatchStatus.COMPLETED);
        }

        // 실패 항목: FAILED + 에러 메시지
        if (!failed.isEmpty()) {
            List<Object[]> failedData = failed.stream()
                    .map(f -> new Object[]{f.bookId(), f.reason()})
                    .toList();
            batchRepository.bulkUpdateEnrichmentFailed(failedData);
        }
    }
}
