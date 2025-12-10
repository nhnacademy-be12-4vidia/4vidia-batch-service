package com.nhnacademy.book_data_batch.batch.enrichment.embedding.tasklet;

import com.nhnacademy.book_data_batch.batch.enrichment.aladin.dto.EnrichmentSuccessDto;
import com.nhnacademy.book_data_batch.batch.enrichment.context.EnrichmentResultsHolder;
import com.nhnacademy.book_data_batch.batch.enrichment.embedding.dto.BookEmbeddingTarget;
import com.nhnacademy.book_data_batch.infrastructure.repository.BatchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;

@Slf4j
@RequiredArgsConstructor
public class EmbeddingPrepareTasklet implements Tasklet {

    private final BatchRepository batchRepository;
    private final EnrichmentResultsHolder resultsHolder;

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        // 1. Aladin 결과 조회
        ConcurrentLinkedQueue<EnrichmentSuccessDto> aladinResults = resultsHolder.getAladinSuccessResults();

        if (aladinResults.isEmpty()) {
            log.debug("[EMBEDDING PREPARE] 처리할 Aladin 결과 없음");
            return RepeatStatus.FINISHED;
        }

        log.info("[EMBEDDING PREPARE] 병합 대상: {}건", aladinResults.size());

        // 2. bookId로 DB 기본 정보 조회 (JDBC 배치 분할)
        List<Long> bookIds = aladinResults.stream()
                .map(EnrichmentSuccessDto::bookId)
                .toList();
        
        List<BookEmbeddingTarget> basicInfoList = batchRepository
                .findEmbeddingTargetsByBookIdsWithBatching(bookIds);
        
        Map<Long, BookEmbeddingTarget> basicInfoMap = new HashMap<>();
        for (BookEmbeddingTarget target : basicInfoList) {
            basicInfoMap.put(target.bookId(), target);
        }

        // 3. Aladin 결과와 DB 기본 정보 병합
        ConcurrentLinkedQueue<BookEmbeddingTarget> targets = resultsHolder.getEmbeddingTargets();
        
        for (EnrichmentSuccessDto aladinResult : aladinResults) {
            BookEmbeddingTarget basicInfo = basicInfoMap.get(aladinResult.bookId());
            
            if (basicInfo == null) {
                log.warn("[EMBEDDING PREPARE] DB에서 기본 정보를 찾을 수 없음: bookId={}", aladinResult.bookId());
                continue;
            }
            
            // 병합: DB 기본 정보 + Aladin 보강 정보
            BookEmbeddingTarget merged = new BookEmbeddingTarget(
                    basicInfo.bookId(),
                    basicInfo.batchId(),
                    basicInfo.isbn(),
                    basicInfo.title(),
                    aladinResult.description() != null ? aladinResult.description() : basicInfo.description(),
                    basicInfo.publisher(),
                    basicInfo.priceSales(),
                    basicInfo.stock(),
                    formatAuthors(aladinResult.authors()),
                    formatTags(aladinResult.tags())
            );
            
            targets.add(merged);
        }

        if (targets.isEmpty()) {
            log.warn("[EMBEDDING PREPARE] 병합된 결과가 없음");
            return RepeatStatus.FINISHED;
        }

        log.info("[EMBEDDING PREPARE] 완료 - 병합된 대상: {}건", targets.size());
        return RepeatStatus.FINISHED;
    }

    /**
     * AuthorWithRole 리스트를 쉼표 구분 문자열로 변환
     */
    private String formatAuthors(List<EnrichmentSuccessDto.AuthorWithRole> authors) {
        if (authors == null || authors.isEmpty()) {
            return "";
        }
        return authors.stream()
                .map(EnrichmentSuccessDto.AuthorWithRole::name)
                .reduce((a, b) -> a + ", " + b)
                .orElse("");
    }

    /**
     * 태그 리스트를 쉼표 구분 문자열로 변환
     */
    private String formatTags(List<String> tags) {
        if (tags == null || tags.isEmpty()) {
            return "";
        }
        return String.join(", ", tags);
    }
}
