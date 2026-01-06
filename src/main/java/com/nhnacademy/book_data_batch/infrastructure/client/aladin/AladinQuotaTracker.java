package com.nhnacademy.book_data_batch.infrastructure.client.aladin;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * API 쿼터 추적기
 */
@Slf4j
@Component
public class AladinQuotaTracker {

    private final Map<String, AtomicInteger> usageMap = new ConcurrentHashMap<>();

    @Getter
    private final int quotaPerKey;

    public AladinQuotaTracker(@Value("${aladin.api.quota-per-key}") int quotaPerKey) {
        this.quotaPerKey = quotaPerKey;
        log.info("[AladinQuotaTracker] 초기화 - 키당 쿼터: {}", quotaPerKey);
    }

    /**
     * 모든 API 키의 쿼터 소진 여부 확인
     *
     * @param apiKeys API 키 목록
     * @return 모든 키가 소진되었으면 true
     */
    public boolean isAllKeysExhausted(java.util.List<String> apiKeys) {
        return apiKeys.stream()
                .allMatch(this::isKeyExhausted);
    }

    /**
     * 특정 API 키의 쿼터 소진 여부 확인
     *
     * @param apiKey API 키
     * @return 키가 소진되었으면 true
     */
    public boolean isKeyExhausted(String apiKey) {
        AtomicInteger counter = usageMap.get(apiKey);
        return counter != null && counter.get() >= quotaPerKey;
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
     * 쿼터 복구
     * - API 호출 실패 시 사용량 감소
     *
     * @param apiKey API 키
     */
    public void releaseQuota(String apiKey) {
        AtomicInteger counter = usageMap.get(apiKey);
        if (counter != null && counter.get() > 0) {
            int before = counter.getAndDecrement();
            log.debug("[AladinQuotaTracker] 쿼터 복구 - Key: {}, 전: {}, 후: {}", 
                apiKey, before, counter.get());
        }
    }

    /**
     * 쿼터 초기화 (Job 시작 시 호출)
     */
    public void reset() {
        usageMap.clear();
        log.info("[AladinQuotaTracker] 쿼터 초기화 완료");
    }
}
