package com.nhnacademy.book_data_batch.batch.components.provider.aladin.tasklet;

import com.nhnacademy.book_data_batch.batch.components.domain.book.dto.BookImageDto;
import com.nhnacademy.book_data_batch.batch.components.provider.aladin.dto.EnrichmentFailureDto;
import com.nhnacademy.book_data_batch.batch.components.provider.aladin.dto.EnrichmentSuccessDto;
import com.nhnacademy.book_data_batch.batch.components.provider.aladin.dto.BookAuthorDto;
import com.nhnacademy.book_data_batch.batch.components.core.context.EnrichmentResultsHolder;
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
import java.util.stream.Collectors;

/**
 * Aladin API 결과 저장 Tasklet
 * 이전 Step에서 수집한 API 결과를 DB에 저장
 * 트랜잭션으로 관리됨
 */
@Slf4j
@RequiredArgsConstructor
public class AladinSaveTasklet implements Tasklet {

    private final AuthorRepository authorRepository;
    private final BookAuthorRepository bookAuthorRepository;
    private final TagRepository tagRepository;
    private final BookTagRepository bookTagRepository;
    private final BookRepository bookRepository;
    private final BookImageRepository bookImageRepository;
    private final BatchRepository batchRepository;
    private final EnrichmentResultsHolder resultsHolder;

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {

        // 결과 조회
        List<EnrichmentSuccessDto> successList = new ArrayList<>(
            resultsHolder.getAladinSuccessResults()
        );
        List<EnrichmentFailureDto> failedList = new ArrayList<>(
            resultsHolder.getAladinFailedResults()
        );

        if (successList.isEmpty() && failedList.isEmpty()) {
            log.debug("[ALADIN] 저장할 데이터 없음");
            return RepeatStatus.FINISHED;
        }

        // DB에 저장
        saveAuthors(successList);
        saveTags(successList);
        saveBooks(successList);
        saveImages(successList);
        updateBatchStatus(successList, failedList);

        // 처리 건수 기록
        contribution.incrementWriteCount(successList.size());
        log.info("[ALADIN SAVE] 완료 - 성공: {}, 실패: {}", successList.size(), failedList.size());

        return RepeatStatus.FINISHED;
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

        // 2. Author bulk insert
        authorRepository.bulkInsert(authorNames);

        // 3. Author ID 조회 (JDBC로 직접 조회하여 MySQL 메타데이터 캐시 오류 회피)
        Map<String, Long> authorIdMap = authorRepository.findIdsByNames(authorNames, 500);

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
        bookImageRepository.bulkInsert(images);
    }

    /**
     * Batch 상태 업데이트
     */
    private void updateBatchStatus(List<EnrichmentSuccessDto> success, List<EnrichmentFailureDto> failed) {
        // 성공 항목: COMPLETED
        if (!success.isEmpty()) {
            List<Long> successBatchIds = success.stream()
                    .map(EnrichmentSuccessDto::batchId)
                    .toList();
            batchRepository.bulkUpdateEnrichmentStatus(successBatchIds, BatchStatus.COMPLETED);
        }

        // 실패 항목: FAILED + 에러 메시지
        if (!failed.isEmpty()) {
            List<Object[]> failedData = failed.stream()
                    .map(f -> new Object[]{f.batchId(), f.reason()})
                    .toList();
            batchRepository.bulkUpdateEnrichmentFailed(failedData);
        }
    }
}
