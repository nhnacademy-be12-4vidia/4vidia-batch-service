package com.nhnacademy.book_data_batch.infrastructure.client.aladin;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("AladinQuotaTracker 테스트")
class AladinQuotaTrackerTest {

    private AladinQuotaTracker tracker;
    private static final int QUOTA_PER_KEY = 100;
    private static final String KEY_1 = "key-1";
    private static final String KEY_2 = "key-2";
    private static final String KEY_3 = "key-3";

    @BeforeEach
    void setUp() {
        tracker = new AladinQuotaTracker(QUOTA_PER_KEY);
    }

    @Test
    @DisplayName("tryAcquire() - 초기 상태에서 쿼터 확보 성공")
    void tryAcquire_initialState_shouldSucceed() {
        boolean result = tracker.tryAcquire(KEY_1);

        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("tryAcquire() - 쿼터 한계까지 사용 가능")
    void tryAcquire_upToLimit_shouldSucceed() {
        for (int i = 0; i < QUOTA_PER_KEY; i++) {
            assertThat(tracker.tryAcquire(KEY_1)).isTrue();
        }
    }

    @Test
    @DisplayName("tryAcquire() - 쿼터 한계 초과 시 실패")
    void tryAcquire_exceedLimit_shouldFail() {
        for (int i = 0; i < QUOTA_PER_KEY; i++) {
            tracker.tryAcquire(KEY_1);
        }

        boolean result = tracker.tryAcquire(KEY_1);
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("tryAcquire() - 쿼터 초과 후 사용량 증가되지 않음")
    void tryAcquire_exceedLimit_shouldNotIncrement() {
        for (int i = 0; i <= QUOTA_PER_KEY; i++) {
            tracker.tryAcquire(KEY_1);
        }

        boolean result = tracker.tryAcquire(KEY_1);
        assertThat(result).isFalse();

        boolean nextResult = tracker.tryAcquire(KEY_1);
        assertThat(nextResult).isFalse();
    }

    @Test
    @DisplayName("isKeyExhausted() - 초기 상태에서 false")
    void isKeyExhausted_initialState_shouldReturnFalse() {
        boolean result = tracker.isKeyExhausted(KEY_1);

        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("isKeyExhausted() - 쿼터 한계 도달 시 true")
    void isKeyExhausted_atLimit_shouldReturnTrue() {
        for (int i = 0; i < QUOTA_PER_KEY; i++) {
            tracker.tryAcquire(KEY_1);
        }

        boolean result = tracker.isKeyExhausted(KEY_1);
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("isKeyExhausted() - 쿼터 한계 전 false")
    void isKeyExhausted_beforeLimit_shouldReturnFalse() {
        for (int i = 0; i < QUOTA_PER_KEY - 1; i++) {
            tracker.tryAcquire(KEY_1);
        }

        boolean result = tracker.isKeyExhausted(KEY_1);
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("isAllKeysExhausted() - 모든 키 소진 시 true")
    void isAllKeysExhausted_allExhausted_shouldReturnTrue() {
        for (int i = 0; i < QUOTA_PER_KEY; i++) {
            tracker.tryAcquire(KEY_1);
            tracker.tryAcquire(KEY_2);
            tracker.tryAcquire(KEY_3);
        }

        List<String> apiKeys = List.of(KEY_1, KEY_2, KEY_3);
        boolean result = tracker.isAllKeysExhausted(apiKeys);

        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("isAllKeysExhausted() - 하나의 키만 소진 시 false")
    void isAllKeysExhausted_oneExhausted_shouldReturnFalse() {
        for (int i = 0; i < QUOTA_PER_KEY; i++) {
            tracker.tryAcquire(KEY_1);
        }

        List<String> apiKeys = List.of(KEY_1, KEY_2, KEY_3);
        boolean result = tracker.isAllKeysExhausted(apiKeys);

        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("isAllKeysExhausted() - 초기 상태에서 false")
    void isAllKeysExhausted_initialState_shouldReturnFalse() {
        List<String> apiKeys = List.of(KEY_1, KEY_2, KEY_3);
        boolean result = tracker.isAllKeysExhausted(apiKeys);

        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("releaseQuota() - 쿼터 감소")
    void releaseQuota_shouldDecrementUsage() {
        for (int i = 0; i < 10; i++) {
            tracker.tryAcquire(KEY_1);
        }

        tracker.releaseQuota(KEY_1);

        boolean result = tracker.tryAcquire(KEY_1);
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("releaseQuota() - 사용량 0일 때 감소하지 않음")
    void releaseQuota_atZero_shouldNotDecrement() {
        tracker.releaseQuota(KEY_1);

        boolean result = tracker.tryAcquire(KEY_1);
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("releaseQuota() - 쿼터 한계 후 복구")
    void releaseQuota_afterExhaustion_shouldAllowNewUsage() {
        for (int i = 0; i < QUOTA_PER_KEY; i++) {
            tracker.tryAcquire(KEY_1);
        }

        boolean exhaustedResult = tracker.tryAcquire(KEY_1);
        assertThat(exhaustedResult).isFalse();

        tracker.releaseQuota(KEY_1);

        boolean newResult = tracker.tryAcquire(KEY_1);
        assertThat(newResult).isTrue();
    }
}