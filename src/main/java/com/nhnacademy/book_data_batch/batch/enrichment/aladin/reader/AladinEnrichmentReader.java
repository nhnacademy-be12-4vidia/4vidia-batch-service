package com.nhnacademy.book_data_batch.batch.enrichment.aladin.reader;

import com.nhnacademy.book_data_batch.batch.enrichment.aladin.dto.BookEnrichmentTarget;
import com.nhnacademy.book_data_batch.entity.enums.BatchStatus;
import com.nhnacademy.book_data_batch.repository.BatchRepository;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.data.RepositoryItemReader;
import org.springframework.batch.item.data.builder.RepositoryItemReaderBuilder;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.Map;

/**
 * Aladin 보강 배치용 Reader (ID 범위 기반)
 * - 파티션 범위 내 PENDING 도서를 조회
 */
@Slf4j
@UtilityClass
public class AladinEnrichmentReader {

    private static final int PAGE_SIZE = 100;

    /**
     * 파티션 범위에 해당하는 PENDING Book 조회용 Reader 생성
     *
     * @param repository BatchRepository
     * @param startId    시작 batch_id (inclusive)
     * @param endId      종료 batch_id (exclusive)
     * @return RepositoryItemReader
     */
    public static RepositoryItemReader<BookEnrichmentTarget> create(
            BatchRepository repository, 
            Long startId, 
            Long endId) {

        log.debug("RepositoryItemReader 생성: startId={}, endId={}", startId, endId);

        return new RepositoryItemReaderBuilder<BookEnrichmentTarget>()
                .name("aladinEnrichmentReader")
                .repository(repository)
                .methodName("findPendingForEnrichment")
                .arguments(List.of(BatchStatus.PENDING, startId, endId))
                .sorts(Map.of("id", Sort.Direction.ASC))
                .pageSize(PAGE_SIZE)
                .build();
    }
}
