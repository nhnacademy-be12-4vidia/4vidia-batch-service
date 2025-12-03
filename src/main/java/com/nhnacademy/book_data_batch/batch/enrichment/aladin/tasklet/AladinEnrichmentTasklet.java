package com.nhnacademy.book_data_batch.batch.enrichment.aladin.tasklet;

import com.nhnacademy.book_data_batch.batch.book.dto.BookImageDto;
import com.nhnacademy.book_data_batch.batch.enrichment.aladin.client.AladinApiClient;
import com.nhnacademy.book_data_batch.batch.enrichment.aladin.client.QuotaTracker;
import com.nhnacademy.book_data_batch.batch.enrichment.aladin.dto.*;
import com.nhnacademy.book_data_batch.batch.enrichment.aladin.mapper.AladinDataMapper;
import com.nhnacademy.book_data_batch.entity.enums.BatchStatus;
import com.nhnacademy.book_data_batch.entity.enums.ImageType;
import com.nhnacademy.book_data_batch.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;
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

    private final QuotaTracker quotaTracker;
    private final AladinApiClient aladinApiClient;
    private final AladinDataMapper aladinDataMapper;
    private final List<String> aladinApiKeys;

    private final AtomicReference<List<BookEnrichmentTarget>> pendingTargets =
            new AtomicReference<>(Collections.emptyList());
    private final ConcurrentLinkedQueue<AladinEnrichmentData> successResults =
            new ConcurrentLinkedQueue<>();
    private final ConcurrentLinkedQueue<FailedEnrichment> failedResults =
            new ConcurrentLinkedQueue<>();

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {

        // 0. 쿼터 초기화
        quotaTracker.reset(); // 이거 필요한가?

        // 1. PENDING 상태 전체 조회 (Projection)
        pendingTargets.set(batchRepository.findAllPending());
        if (pendingTargets.get().isEmpty()) {
            return RepeatStatus.FINISHED;
        }

        // 2. API 키 수만큼 파티션으로 분할
        List<List<BookEnrichmentTarget>> partitions = partition(pendingTargets.get(), aladinApiKeys.size());

        // 3. Virtual Threads로 병렬 처리
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<Future<?>> futures = new ArrayList<>();

            for (int i = 0; i < partitions.size(); i++) {
                final int partitionIdx = i;
                final List<BookEnrichmentTarget> partition = partitions.get(i);
                final String apiKey = aladinApiKeys.get(i);

                futures.add(executor.submit(() -> processPartition(partition, apiKey, partitionIdx)));
            }

            // 모든 가상 스레드 완료 대기
            for (Future<?> future : futures) {
                try {
                    future.get();
                } catch (Exception e) {
                    log.error("[TASKLET] 파티션 처리 중 오류: {}", e.getMessage());
                }
            }

        } finally {
            log.info("[TASKLET] 알라딘 API 호출 완료 - 성공: {}, 실패: {}",
                    successResults.size(), failedResults.size());
        }

        // 4. Bulk 판단
        if (successResults.isEmpty() && failedResults.isEmpty()) {
            log.info("[TASKLET] 저장할 데이터 없음");
            return RepeatStatus.FINISHED;
        }
        List<AladinEnrichmentData> successList = new ArrayList<>(successResults);

        // 5. 처리
        saveAuthors(successList);
        saveTags(successList);
        saveBooks(successList);
        saveImages(successList);
        updateBatchStatus(successList, new ArrayList<>(failedResults));

        // 6. 처리 건수 기록
        contribution.incrementWriteCount(successList.size());
        log.info("[TASKLET] 알라딘 데이터 저장 완료 - 성공: {}, 실패: {}",
                successList.size(), failedResults.size());

        return RepeatStatus.FINISHED;
    }

    private <T> List<List<T>> partition(List<T> list, int n) {
        List<List<T>> partitions = new ArrayList<>();
        int size = list.size();
        int partitionSize = (size + n - 1) / n;  // 올림 나눗셈

        for (int i = 0; i < n; i++) {
            int start = i * partitionSize;
            int end = Math.min(start + partitionSize, size);

            if (start < size) {
                partitions.add(new ArrayList<>(list.subList(start, end)));
            } else {
                partitions.add(new ArrayList<>());  // 빈 파티션
            }
        }

        return partitions;
    }

    private void processPartition(
            List<BookEnrichmentTarget> targets,
            String apiKey,
            int partitionIdx) {

        for (BookEnrichmentTarget target : targets) {

            // ISBN 체크
            if (!StringUtils.hasText(target.isbn13())) {
                failedResults.add(new FailedEnrichment(target.bookId(), target.batchId(), "ISBN13 없음"));
                continue;
            }

            // 쿼터 체크
            if (!quotaTracker.tryAcquire(apiKey)) {
                break;
            }

            try {
                Optional<AladinItemDto> response = aladinApiClient.lookupByIsbn(target.isbn13(), apiKey);

                if (response.isPresent()) {
                    AladinEnrichmentData data = aladinDataMapper.map(target, response.get());
                    successResults.add(data);
                } else {
                    failedResults.add(new FailedEnrichment(target.bookId(), target.batchId(), "API 응답 없음"));
                }
            } catch (Exception e) {
                failedResults.add(new FailedEnrichment(target.bookId(), target.batchId(), e.getMessage()));
            }
        }

        log.info("[TASKLET] 알라딘 API 파티션 {} 완료 - 성공: {}, 실패: {}",
                partitionIdx,
                successResults.size(),
                failedResults.size());
    }

    /**
     * Author 저장 및 BookAuthor 관계 생성
     */
    private void saveAuthors(List<AladinEnrichmentData> results) {
        // 1. 모든 저자 이름 수집 (중복 제거)
        Set<String> authorNames = results.stream()
                .filter(AladinEnrichmentData::hasAuthors)
                .flatMap(r -> r.authors().stream())
                .map(AladinEnrichmentData.AuthorWithRole::name)
                .collect(Collectors.toSet());

        if (authorNames.isEmpty()) {
            log.debug("[Step3] 저장할 Author 없음");
            return;
        }

        log.info("[Step3] Author 저장 시작 - {}명", authorNames.size());

        // 2. Author bulk insert (INSERT IGNORE)
        authorRepository.bulkInsert(authorNames);

        // 3. Author ID 조회
        Map<String, Long> authorIdMap = authorRepository.findIdsByNames(authorNames);

        // 4. BookAuthor 관계 생성
        List<BookAuthorDto> bookAuthors = new ArrayList<>();
        for (AladinEnrichmentData data : results) {
            if (!data.hasAuthors()) continue;

            for (AladinEnrichmentData.AuthorWithRole author : data.authors()) {
                Long authorId = authorIdMap.get(author.name());
                if (authorId != null) {
                    bookAuthors.add(new BookAuthorDto(data.bookId(), authorId, author.role()));
                }
            }
        }

        // 5. BookAuthor bulk insert
        if (!bookAuthors.isEmpty()) {
            bookAuthorRepository.bulkInsert(bookAuthors);
            log.info("[Step3] BookAuthor 저장 완료 - {}건", bookAuthors.size());
        }
    }

    /**
     * Tag 저장 및 BookTag 관계 생성
     */
    private void saveTags(List<AladinEnrichmentData> results) {
        // 1. 모든 태그 이름 수집 (중복 제거)
        Set<String> tagNames = results.stream()
                .filter(AladinEnrichmentData::hasTags)
                .flatMap(r -> r.tags().stream())
                .collect(Collectors.toSet());

        if (tagNames.isEmpty()) {
            log.debug("[Step3] 저장할 Tag 없음");
            return;
        }

        log.info("[Step3] Tag 저장 시작 - {}개", tagNames.size());

        // 2. Tag bulk insert (INSERT IGNORE)
        tagRepository.bulkInsert(tagNames);

        // 3. Tag ID 조회
        Map<String, Long> tagIdMap = tagRepository.findIdsByNames(tagNames);

        // 4. BookTag 관계 생성
        List<long[]> bookTagPairs = new ArrayList<>();
        for (AladinEnrichmentData data : results) {
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
            log.info("[Step3] BookTag 저장 완료 - {}건", bookTagPairs.size());
        }
    }

    /**
     * Book 엔티티 업데이트
     */
    private void saveBooks(List<AladinEnrichmentData> results) {
        if (results.isEmpty()) {
            return;
        }

        log.info("[Step3] Book 업데이트 시작 - {}건", results.size());
        bookRepository.bulkUpdateFromEnrichment(results);
        log.info("[Step3] Book 업데이트 완료");
    }

    /**
     * BookImage 저장
     */
    private void saveImages(List<AladinEnrichmentData> results) {
        List<BookImageDto> images = results.stream()
                .filter(AladinEnrichmentData::hasCoverUrl)
                .map(r -> new BookImageDto(
                        r.bookId(),
                        r.coverUrl(),
                        ImageType.THUMBNAIL.getCode(),
                        0  // displayOrder
                ))
                .toList();

        if (images.isEmpty()) {
            log.debug("[Step3] 저장할 BookImage 없음");
            return;
        }

        log.info("[Step3] BookImage 저장 시작 - {}건", images.size());
        bookImageRepository.bulkInsert(images);
        log.info("[Step3] BookImage 저장 완료");
    }

    /**
     * Batch 상태 업데이트
     */
    private void updateBatchStatus(List<AladinEnrichmentData> success, List<FailedEnrichment> failed) {
        // 성공 항목: COMPLETED
        if (!success.isEmpty()) {
            List<Long> successBookIds = success.stream()
                    .map(AladinEnrichmentData::bookId)
                    .toList();
            batchRepository.bulkUpdateEnrichmentStatus(successBookIds, BatchStatus.COMPLETED);
            log.info("[Step3] Batch 상태 업데이트 (성공) - {}건", successBookIds.size());
        }

        // 실패 항목: FAILED + 에러 메시지
        if (!failed.isEmpty()) {
            List<Object[]> failedData = failed.stream()
                    .map(f -> new Object[]{f.bookId(), f.reason()})
                    .toList();
            batchRepository.bulkUpdateEnrichmentFailed(failedData);
            log.info("[Step3] Batch 상태 업데이트 (실패) - {}건", failedData.size());
        }
    }
}
