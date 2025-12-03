package com.nhnacademy.book_data_batch.batch.enrichment.common;

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
public class QuotaTracker {

    private final Map<String, AtomicInteger> usageMap = new ConcurrentHashMap<>();

    @Getter
    private final int quotaPerKey;

    public QuotaTracker(@Value("${aladin.api.quota-per-key}") int quotaPerKey) {
        this.quotaPerKey = quotaPerKey;
        log.info("[QuotaTracker] 초기화 - 키당 쿼터: {}", quotaPerKey);
    }

    /**
     * API 키 사용 가능 여부 확인
     * 
     * @param apiKey API 키
     * @return 사용 가능하면 true
     */
    public boolean canUse(String apiKey) {
        return getUsage(apiKey) < quotaPerKey;
    }

    /**
     * API 호출 카운트 증가 및 현재 값 반환
     * 
     * @param apiKey API 키
     * @return 증가 후 사용량
     */
    public int incrementAndGet(String apiKey) {
        return usageMap.computeIfAbsent(apiKey, k -> new AtomicInteger(0))
                       .incrementAndGet();
    }

    /**
     * 현재 사용량 조회
     * 
     * @param apiKey API 키
     * @return 현재 사용량
     */
    public int getUsage(String apiKey) {
        AtomicInteger usage = usageMap.get(apiKey);
        return usage != null ? usage.get() : 0;
    }

    /**
     * 남은 쿼터 조회
     * 
     * @param apiKey API 키
     * @return 남은 쿼터
     */
    public int getRemainingQuota(String apiKey) {
        return Math.max(0, quotaPerKey - getUsage(apiKey));
    }

    /**
     * 전체 사용량 요약 조회
     * 
     * @return 전체 사용량 합계
     */
    public int getTotalUsage() {
        return usageMap.values().stream()
                .mapToInt(AtomicInteger::get)
                .sum();
    }

    /**
     * 쿼터 초기화 (Job 시작 시 호출)
     */
    public void reset() {
        usageMap.clear();
        log.info("[QuotaTracker] 쿼터 초기화 완료");
    }

    /**
     * 현재 사용량 로그 출력
     */
    public void logUsage() {
        log.info("[QuotaTracker] 사용량 현황:");
        usageMap.forEach((key, usage) -> {
            String maskedKey = key.length() > 8 ? key.substring(0, 8) + "***" : key;
            log.info("  - {}: {}/{}", maskedKey, usage.get(), quotaPerKey);
        });
        log.info("  - 총합: {}", getTotalUsage());
    }
}
