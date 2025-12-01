package com.nhnacademy.book_data_batch.service.openapi;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Aladin API 일일 호출 쿼터 관리 서비스
 * - Redis에 API 키별 일일 사용량 저장
 * - 자정에 자동 만료 (TTL)
 * - 여러 서버/Job 인스턴스에서 공유 가능
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AladinQuotaService {

    private static final int DAILY_QUOTA_LIMIT = 5000;
    private static final String KEY_PREFIX = "aladin:quota:";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final StringRedisTemplate redisTemplate;

    /**
     * API 호출 횟수 증가 및 현재 사용량 반환
     *
     * @param apiKey API 키
     * @return 증가 후 현재 사용량
     */
    public long incrementAndGet(String apiKey) {
        String key = buildKey(apiKey);
        Long count = redisTemplate.opsForValue().increment(key);
        
        // 첫 호출 시 TTL 설정 (자정까지)
        if (count != null && count == 1) {
            Duration ttl = calculateTtlUntilMidnight();
            redisTemplate.expire(key, ttl);
            log.debug("Redis 쿼터 키 생성: {}, TTL: {}", key, ttl);
        }
        
        return count != null ? count : 0;
    }

    /**
     * 현재 사용량 조회
     *
     * @param apiKey API 키
     * @return 현재 사용량 (없으면 0)
     */
    public long getCurrentUsage(String apiKey) {
        String key = buildKey(apiKey);
        String value = redisTemplate.opsForValue().get(key);
        return value != null ? Long.parseLong(value) : 0;
    }

    /**
     * 쿼터 초과 여부 확인
     *
     * @param apiKey API 키
     * @return 초과 시 true
     */
    public boolean isQuotaExceeded(String apiKey) {
        return getCurrentUsage(apiKey) >= DAILY_QUOTA_LIMIT;
    }

    /**
     * 남은 쿼터 조회
     *
     * @param apiKey API 키
     * @return 남은 호출 가능 횟수
     */
    public long getRemainingQuota(String apiKey) {
        long current = getCurrentUsage(apiKey);
        return Math.max(0, DAILY_QUOTA_LIMIT - current);
    }

    public int getDailyQuotaLimit() {
        return DAILY_QUOTA_LIMIT;
    }

    /**
     * Redis 키 생성: aladin:quota:2025-12-01:ttbxxx
     */
    private String buildKey(String apiKey) {
        String today = LocalDate.now().format(DATE_FORMATTER);
        String keyShort = apiKey.length() > 8 ? apiKey.substring(0, 8) : apiKey;
        return KEY_PREFIX + today + ":" + keyShort;
    }

    /**
     * 자정까지 남은 시간 계산
     */
    private Duration calculateTtlUntilMidnight() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime midnight = now.toLocalDate().plusDays(1).atStartOfDay();
        return Duration.between(now, midnight);
    }
}
