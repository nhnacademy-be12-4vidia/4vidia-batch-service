package com.nhnacademy.book_data_batch.batch.book.partitioner;

import com.nhnacademy.book_data_batch.batch.book.BookDataJobConfig;
import org.springframework.batch.core.partition.support.Partitioner;
import org.springframework.batch.item.ExecutionContext;

import java.util.HashMap;
import java.util.Map;

/**
 * RangePartitioner
 * - 전체 CSV 파일을 N개의 범위로 균등 분할
 * - 각 파티션에 (startRow, endRow) 정보 전달
 * 
 * [ExecutionContext 키]
 * - startRow: 시작 행 번호 (0-based, inclusive)
 * - endRow: 종료 행 번호 (exclusive)
 * - partitionNumber: 파티션 인덱스 (0, 1, 2, ...)
 * 
 * @see BookDataJobConfig masterStep에서 이 Partitioner 사용
 */
public class RangePartitioner implements Partitioner {

    private final int totalRows; // CSV 파일의 총 데이터 행 수 (헤더 제외)

    /**
     * RangePartitioner 생성
     * 
     * @param totalRows CSV 파일의 총 데이터 행 수 (헤더 제외)
     */
    public RangePartitioner(int totalRows) {
        this.totalRows = totalRows;
    }

    /**
     * CSV 파일을 gridSize개의 범위로 분할
     * 
     * @param gridSize 분할할 파티션 수
     * @return 파티션 이름 → ExecutionContext 매핑
     */
    @Override
    public Map<String, ExecutionContext> partition(int gridSize) {
        Map<String, ExecutionContext> result = new HashMap<>();

        // 각 파티션이 담당할 행 수 (올림 처리로 마지막 파티션 누락 방지)
        int partitionSize = (int) Math.ceil((double) totalRows / gridSize);

        for (int i = 0; i < gridSize; i++) {
            ExecutionContext context = new ExecutionContext();

            // 이 파티션의 시작/종료 행 계산
            int startRow = i * partitionSize;
            int endRow = Math.min((i + 1) * partitionSize, totalRows);

            // ExecutionContext에 범위 정보 저장
            // → @StepScope Bean에서 SpEL로 주입받아 사용
            context.putInt("startRow", startRow);
            context.putInt("endRow", endRow);
            context.putInt("partitionNumber", i);

            // 파티션 이름: "partition0", "partition1", ...
            result.put("partition" + i, context);
        }

        return result;
    }
}
