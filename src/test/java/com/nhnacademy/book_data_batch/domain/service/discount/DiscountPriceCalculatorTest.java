package com.nhnacademy.book_data_batch.domain.service.discount;

import com.nhnacademy.book_data_batch.domain.entity.DiscountPolicy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("DiscountPriceCalculator 테스트")
class DiscountPriceCalculatorTest {

    private DiscountPriceCalculator calculator;

    @BeforeEach
    void setUp() {
        calculator = new DiscountPriceCalculator();
    }

    @Test
    @DisplayName("정가 null 시 null 반환")
    void calculate_nullPriceStandard_returnsNull() {
        Integer result = calculator.calculate(null, 10);
        assertNull(result);
    }

    @ParameterizedTest
    @CsvSource({
            "10000, 0, 10000",
            "10000, 10, 9000",
            "10000, 20, 8000",
            "10000, 50, 5000",
            "10000, 100, 0"
    })
    @DisplayName("할인율별 올바른 할인가 계산")
    void calculate_variousRates_returnsCorrectPrice(int price, int rate, int expected) {
        Integer result = calculator.calculate(price, rate);
        assertEquals(expected, result);
    }

    @Test
    @DisplayName("음수 할인율은 0으로 처리")
    void calculate_negativeRate_clampedToZero() {
        Integer result = calculator.calculate(10000, -10);
        assertEquals(10000, result);
    }

    @Test
    @DisplayName("100% 초과 할인율은 100으로 처리")
    void calculate_rateOver100_clampedToHundred() {
        Integer result = calculator.calculate(10000, 150);
        assertEquals(0, result);
    }

    @Test
    @DisplayName("할인가 계산 시 내림 처리 확인")
    void calculate_floorRounding_appliesFloor() {
        Integer result = calculator.calculate(1000, 33);
        assertEquals(670, result);
    }

    @Test
    @DisplayName("정가 null 시 기본 할인 계산 null 반환")
    void calculateDefault_nullPriceStandard_returnsNull() {
        Integer result = calculator.calculateDefault(null);
        assertNull(result);
    }

    @Test
    @DisplayName("기본 10% 할인 계산")
    void calculateDefault_validPrice_returnsDiscountedPrice() {
        Integer result = calculator.calculateDefault(10000);
        assertEquals(9000, result);
    }

    @Test
    @DisplayName("정가 null 시 정책 기반 계산 null 반환")
    void calculateWithPolicy_nullPriceStandard_returnsNull() {
        DiscountPolicy policy = mock(DiscountPolicy.class);
        when(policy.getDiscountRate()).thenReturn(20);
        Integer result = calculator.calculate(null, policy);
        assertNull(result);
    }

    @Test
    @DisplayName("null 정책은 10% 기본 할인 적용")
    void calculateWithPolicy_nullPolicy_appliesDefaultRate() {
        Integer result = calculator.calculate(10000, (DiscountPolicy) null);
        assertEquals(9000, result);
    }

    @Test
    @DisplayName("정책의 할인율 null 시 10% 기본 할인 적용")
    void calculateWithPolicy_policyDiscountRateNull_appliesDefaultRate() {
        DiscountPolicy policy = mock(DiscountPolicy.class);
        when(policy.getDiscountRate()).thenReturn(null);
        Integer result = calculator.calculate(10000, policy);
        assertEquals(9000, result);
    }

    @ParameterizedTest
    @CsvSource({
            "10000, 15, 8500",
            "10000, 30, 7000",
            "10000, 5, 9500"
    })
    @DisplayName("정책 기반 할인율별 올바른 할인가 계산")
    void calculateWithPolicy_variousRates_returnsCorrectPrice(int price, int rate, int expected) {
        DiscountPolicy policy = mock(DiscountPolicy.class);
        when(policy.getDiscountRate()).thenReturn(rate);
        Integer result = calculator.calculate(price, policy);
        assertEquals(expected, result);
    }
}
