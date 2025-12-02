package com.nhnacademy.book_data_batch.batch.enrichment.partitioner;

import com.nhnacademy.book_data_batch.entity.enums.BatchStatus;
import com.nhnacademy.book_data_batch.repository.BatchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.partition.support.Partitioner;
import org.springframework.batch.item.ExecutionContext;

import java.util.HashMap;
import java.util.Map;

/**
 *
 * 
 * <p>QueryDSL을 통해 MIN/MAX ID를 조회하고 범위를 분할합니다.
 * 각 파티션은 자신의 범위 내에서 쿼터(5,000건)가 소진될 때까지 처리합니다.</p>
 */
@Slf4j
@RequiredArgsConstructor
public class AladinEnrichmentPartitioner implements Partitioner {

    private final BatchRepository batchRepository;
    private final int gridSize;

    @Override
    public Map<String, ExecutionContext> partition(int gridSize) {
        Map<String, ExecutionContext> partitions = new HashMap<>();

        // PENDING 상태 레코드의 범위 조회
        Long minId = batchRepository.findMinIdByEnrichmentStatus(BatchStatus.PENDING);
        Long maxId = batchRepository.findMaxIdByEnrichmentStatus(BatchStatus.PENDING);
        long totalCount = batchRepository.countByEnrichmentStatus(BatchStatus.PENDING);

        if (minId == null || maxId == null || totalCount == 0) {
            log.info("보강할 도서가 없습니다.");
            return partitions;
        }

        log.info("Aladin 보강 파티셔닝: minId={}, maxId={}, totalCount={}, gridSize={}", 
                minId, maxId, totalCount, this.gridSize);

        // ID 범위 기반 파티셔닝
        long range = maxId - minId + 1;
        long partitionSize = (range + this.gridSize - 1) / this.gridSize;  // 올림 나눗셈

        for (int i = 0; i < this.gridSize; i++) {
            ExecutionContext context = new ExecutionContext();
            
            long startId = minId + (i * partitionSize);
            long endId = Math.min(startId + partitionSize, maxId + 1);

            context.putLong("startId", startId);
            context.putLong("endId", endId);
            context.putInt("partitionIndex", i);  // API 키 인덱스로 사용

            partitions.put("partition" + i, context);
            
            log.debug("파티션 {}: startId={}, endId={}, apiKeyIndex={}", i, startId, endId, i);
        }

        return partitions;
    }
}
