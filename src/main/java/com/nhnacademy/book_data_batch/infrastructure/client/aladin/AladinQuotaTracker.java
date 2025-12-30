package com.nhnacademy.book_data_batch.infrastructure.client.aladin;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean; // Import AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger;

/**
 * API 쿼터 추적기
 */
@Slf4j
@Component
public class AladinQuotaTracker {

    private final Map<String, AtomicInteger> usageMap = new ConcurrentHashMap<>();
    private final AtomicBoolean quotaExhausted = new AtomicBoolean(false); // Global flag

    @Getter
    private final int quotaPerKey;

    public AladinQuotaTracker(@Value("${aladin.api.quota-per-key}") int quotaPerKey) {
        this.quotaPerKey = quotaPerKey;
        log.info("[AladinQuotaTracker] 초기화 - 키당 쿼터: {}", quotaPerKey);
    }

    /**
     * 전체 쿼터 소진 여부 확인
     */
    public boolean isQuotaExhausted() {
        return quotaExhausted.get();
    }

    /**
     * 전체 쿼터 소진 상태 설정
     *
     * @param exhausted 소진 여부
     */
    public void setQuotaExhausted(boolean exhausted) {
        this.quotaExhausted.set(exhausted);
    }

    /**
     * API 키 사용 시도
     * - 사용 성공 시 사용량 1 증가
     *
     * @param apiKey API 키
     * @return 사용 성공하면 true
     */
    public boolean tryAcquire(String apiKey) {
        return usageMap.computeIfAbsent(apiKey, k -> new AtomicInteger(0))
                .getAndUpdate(current -> current < quotaPerKey ? current + 1 : current) < quotaPerKey;
    }

    /**
     * 쿼터 초기화 (Job 시작 시 호출)
     */
    public void reset() {
        usageMap.clear();
        quotaExhausted.set(false); // Reset global flag
        log.info("[AladinQuotaTracker] 쿼터 초기화 완료");
    }
}
