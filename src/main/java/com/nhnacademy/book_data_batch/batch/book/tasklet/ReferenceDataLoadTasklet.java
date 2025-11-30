package com.nhnacademy.book_data_batch.batch.book.tasklet;

import com.nhnacademy.book_data_batch.batch.book.cache.ReferenceDataCache;
import com.nhnacademy.book_data_batch.repository.AuthorRepository;
import com.nhnacademy.book_data_batch.repository.PublisherRepository;
import com.nhnacademy.book_data_batch.service.AuthorNameExtractor;
import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.core.io.Resource;
import org.springframework.util.StringUtils;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * <pre>
 * ReferenceDataLoadTasklet
 * 0. CSV 파일 읽기 준비
 * 1. 파일 전체 스캔 → 작가/출판사 이름 수집
 * 2. Bulk INSERT (INSERT IGNORE)
 * 3. 캐시 구축 (ConcurrentHashMap)
 * 4. RepeatStatus.FINISHED 반환
 * </pre>
 */
@Slf4j
@RequiredArgsConstructor
public class ReferenceDataLoadTasklet implements Tasklet {

    private final Resource csvResource;
    private final AuthorNameExtractor authorNameExtractor;
    private final AuthorRepository authorRepository;
    private final PublisherRepository publisherRepository;
    private final ReferenceDataCache referenceDataCache;

    private static final int AUTHOR_COLUMN_INDEX = 4;    // 저자 컬럼
    private static final int PUBLISHER_COLUMN_INDEX = 5; // 출판사 컬럼

    /**
     * Tasklet 실행 메서드
     * 1. CSV 파일 전체 스캔 → 작가/출판사 이름 수집
     * 2. DB에 Bulk INSERT (INSERT IGNORE)
     * 3. DB에서 전체 조회 → 캐시 구축
     * 4. RepeatStatus.FINISHED 반환 (다음 Step 진행)
     * 
     * [트랜잭션]
     * - Spring Batch가 자동으로 트랜잭션 관리
     * - 이 메서드 전체가 하나의 트랜잭션
     * 
     * @param contribution Step 실행 정보 (읽기/쓰기 카운트 등)
     * @param chunkContext Chunk 컨텍스트 (사용 안 함)
     * @return RepeatStatus.FINISHED (한 번만 실행)
     * @throws Exception CSV 읽기 또는 DB 작업 실패 시
     */
    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {

        // 1. CSV 파일 전부 읽어서 -> 작가/출판사 이름 수집
        Set<String> authorNames = new HashSet<>();
        Set<String> publisherNames = new HashSet<>();
        int lineCount = 0;

        try (CSVReader csvReader = new CSVReaderBuilder(
                new InputStreamReader(csvResource.getInputStream(), StandardCharsets.UTF_8))
                .withSkipLines(1)  // 헤더 스킵
                .withCSVParser(new CSVParserBuilder()
                        .withSeparator(',')
                        .withQuoteChar('"')
                        .build())
                .build()) {

            String[] columns;
            while ((columns = csvReader.readNext()) != null) {
                lineCount++;

                // 작가 추출
                if (columns.length > AUTHOR_COLUMN_INDEX) {
                    String rawAuthor = columns[AUTHOR_COLUMN_INDEX];
                    Set<String> extracted = authorNameExtractor.extractUniqueAuthors(rawAuthor);
                    authorNames.addAll(extracted);
                }

                // 출판사 추출
                if (columns.length > PUBLISHER_COLUMN_INDEX) {
                    String publisher = columns[PUBLISHER_COLUMN_INDEX].trim();
                    if (StringUtils.hasText(publisher)) {
                        publisherNames.add(publisher);
                    }
                }

                // 진행 상황 로그 (5만 행마다)
                if (lineCount % 50000 == 0) {
                    log.info("[TASKLET] CSV에서 작가/출판사 추출중 -> {}행 처리됨 (작가: {}, 출판사: {})",
                            lineCount, authorNames.size(), publisherNames.size());
                }
            }
        }

        log.info("[TASKLET] CSV 스캔 완료: 총 {}행, 작가 {}명, 출판사 {}개",
                lineCount, authorNames.size(), publisherNames.size());

        // 2. DB에 Bulk INSERT (병렬 처리)
        ExecutorService parallelExecutor = Executors.newFixedThreadPool(2);
        try {
            CompletableFuture<Void> authorFuture = CompletableFuture.runAsync(
                    () -> authorRepository.bulkInsert(authorNames), parallelExecutor);

            CompletableFuture<Void> publisherFuture = CompletableFuture.runAsync(
                    () -> publisherRepository.bulkInsert(publisherNames), parallelExecutor);

            CompletableFuture.allOf(authorFuture, publisherFuture).join();
        } finally {
            parallelExecutor.shutdown();
        }

        // 3. 캐시 구축 (DB에서 전체 조회)
        // 이전 Job 실행의 캐시 데이터가 남아있을 수 있으므로 먼저 초기화
        referenceDataCache.clear();
        referenceDataCache.buildFromRepositories(authorRepository, publisherRepository);

        // StepContribution에 처리 건수 기록: TODO: 통계 만들 때 사용한다는데 일단 이렇게만 두고 나중에...
        contribution.incrementWriteCount(authorNames.size() + publisherNames.size());

        // FINISHED 반환 → 다음 Step으로 진행
        return RepeatStatus.FINISHED;
    }
}
