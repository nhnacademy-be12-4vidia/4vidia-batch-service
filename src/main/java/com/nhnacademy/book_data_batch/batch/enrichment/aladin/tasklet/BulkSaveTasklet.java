package com.nhnacademy.book_data_batch.batch.enrichment.aladin.tasklet;

import com.nhnacademy.book_data_batch.batch.book.dto.BookImageDto;
import com.nhnacademy.book_data_batch.batch.enrichment.aladin.dto.AladinEnrichmentData;
import com.nhnacademy.book_data_batch.batch.enrichment.aladin.dto.AladinEnrichmentData.AuthorWithRole;
import com.nhnacademy.book_data_batch.batch.enrichment.aladin.dto.BookAuthorDto;
import com.nhnacademy.book_data_batch.batch.enrichment.aladin.dto.FailedEnrichment;
import com.nhnacademy.book_data_batch.batch.enrichment.common.EnrichmentCache;
import com.nhnacademy.book_data_batch.entity.enums.BatchStatus;
import com.nhnacademy.book_data_batch.entity.enums.ImageType;
import com.nhnacademy.book_data_batch.repository.AuthorRepository;
import com.nhnacademy.book_data_batch.repository.BatchRepository;
import com.nhnacademy.book_data_batch.repository.BookAuthorRepository;
import com.nhnacademy.book_data_batch.repository.BookImageRepository;
import com.nhnacademy.book_data_batch.repository.BookRepository;
import com.nhnacademy.book_data_batch.repository.BookTagRepository;
import com.nhnacademy.book_data_batch.repository.TagRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Step 3: Bulk 저장
 * - Author → BookAuthor
 * - Tag → BookTag
 * - Book 업데이트
 * - BookImage 저장
 * - Batch 상태 업데이트
 */
@Slf4j
@RequiredArgsConstructor
public class BulkSaveTasklet implements Tasklet {

    private final EnrichmentCache cache;
    private final AuthorRepository authorRepository;
    private final BookAuthorRepository bookAuthorRepository;
    private final TagRepository tagRepository;
    private final BookTagRepository bookTagRepository;
    private final BookRepository bookRepository;
    private final BookImageRepository bookImageRepository;
    private final BatchRepository batchRepository;

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        List<AladinEnrichmentData> successResults = cache.getSuccessResults();
        List<FailedEnrichment> failedResults = cache.getFailedResults();

        log.info("[Step3] Bulk 저장 시작 - 성공: {}, 실패: {}", successResults.size(), failedResults.size());

        if (successResults.isEmpty() && failedResults.isEmpty()) {
            log.info("[Step3] 저장할 데이터 없음");
            return RepeatStatus.FINISHED;
        }

        // 1. Author 처리
        saveAuthors(successResults);

        // 2. Tag 처리
        saveTags(successResults);

        // 3. Book 업데이트
        saveBooks(successResults);

        // 4. BookImage 저장
        saveImages(successResults);

        // 5. Batch 상태 업데이트
        updateBatchStatus(successResults, failedResults);

        // 6. 캐시 정리
        cache.clearResults();

        // 7. 처리 건수 기록
        contribution.incrementWriteCount(successResults.size());

        log.info("[Step3] Bulk 저장 완료 - 성공: {}, 실패: {}", successResults.size(), failedResults.size());

        return RepeatStatus.FINISHED;
    }

    /**
     * Author 저장 및 BookAuthor 관계 생성
     */
    private void saveAuthors(List<AladinEnrichmentData> results) {
        // 1. 모든 저자 이름 수집 (중복 제거)
        Set<String> authorNames = results.stream()
                .filter(AladinEnrichmentData::hasAuthors)
                .flatMap(r -> r.authors().stream())
                .map(AuthorWithRole::name)
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

            for (AuthorWithRole author : data.authors()) {
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
