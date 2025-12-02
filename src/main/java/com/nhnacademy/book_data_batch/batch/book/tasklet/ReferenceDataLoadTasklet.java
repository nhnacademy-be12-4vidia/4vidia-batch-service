package com.nhnacademy.book_data_batch.batch.book.tasklet;

import com.nhnacademy.book_data_batch.batch.book.cache.ReferenceDataCache;
import com.nhnacademy.book_data_batch.repository.PublisherRepository;
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

/**
 * <pre>
 * ReferenceDataLoadTasklet
 * - 출판사 데이터 사전 로딩 (작가는 알라딘 API에서 처리)
 * 
 * 0. CSV 파일 읽기 준비
 * 1. 파일 전체 스캔 → 출판사 이름 수집
 * 2. Bulk INSERT (INSERT IGNORE)
 * 3. 캐시 구축 (ConcurrentHashMap)
 * 4. RepeatStatus.FINISHED 반환
 * </pre>
 */
@Slf4j
@RequiredArgsConstructor
public class ReferenceDataLoadTasklet implements Tasklet {

    private final Resource csvResource;
    private final PublisherRepository publisherRepository;
    private final ReferenceDataCache referenceDataCache;

    private static final int PUBLISHER_COLUMN_INDEX = 5; // 출판사 컬럼

    /**
     * Tasklet 실행 메서드
     * 1. CSV 파일 전체 스캔 → 출판사 이름 수집
     * 2. DB에 Bulk INSERT (INSERT IGNORE)
     * 3. DB에서 전체 조회 → 캐시 구축
     * 4. RepeatStatus.FINISHED 반환 (다음 Step 진행)
     * 
     * @param contribution Step 실행 정보 (읽기/쓰기 카운트 등)
     * @param chunkContext Chunk 컨텍스트 (사용 안 함)
     * @return RepeatStatus.FINISHED (한 번만 실행)
     * @throws Exception CSV 읽기 또는 DB 작업 실패 시
     */
    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {

        // 1. CSV 파일 전부 읽어서 -> 출판사 이름 수집
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

                // 출판사 추출
                if (columns.length > PUBLISHER_COLUMN_INDEX) {
                    String publisher = columns[PUBLISHER_COLUMN_INDEX].trim();
                    if (StringUtils.hasText(publisher)) {
                        publisherNames.add(publisher);
                    }
                }

                // 진행 상황 로그 (5만 행마다)
                if (lineCount % 50000 == 0) {
                    log.info("[TASKLET] CSV에서 출판사 추출중 -> {}행 처리됨 (출판사: {})",
                            lineCount, publisherNames.size());
                }
            }
        }

        log.info("[TASKLET] CSV 스캔 완료: 총 {}행, 출판사 {}개", lineCount, publisherNames.size());

        // 2. DB에 Bulk INSERT
        publisherRepository.bulkInsert(publisherNames);

        // 3. 캐시 구축 (DB에서 전체 조회)
        referenceDataCache.clear();
        referenceDataCache.buildFromRepository(publisherRepository);

        // StepContribution에 처리 건수 기록
        contribution.incrementWriteCount(publisherNames.size());

        // FINISHED 반환 → 다음 Step으로 진행
        return RepeatStatus.FINISHED;
    }
}
