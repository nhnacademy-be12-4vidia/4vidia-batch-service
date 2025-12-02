package com.nhnacademy.book_data_batch.batch.enrichment.exception;

import lombok.Getter;

/**
 * 일일 API 호출 쿼터 초과 시 발생하는 예외
 * - 파티션별로 5,000건 제한
 * - 이 예외 발생 시 해당 파티션의 Step이 중단됨
 */
@Getter
public class DailyQuotaExceededException extends RuntimeException {

    private final int partitionIndex;
    private final int quotaLimit;
    private final int apiCallCount;

    public DailyQuotaExceededException(int partitionIndex, int quotaLimit, int apiCallCount) {
        super(String.format("파티션 %d 일일 쿼터 초과: %d/%d", partitionIndex, apiCallCount, quotaLimit));
        this.partitionIndex = partitionIndex;
        this.quotaLimit = quotaLimit;
        this.apiCallCount = apiCallCount;
    }
}
