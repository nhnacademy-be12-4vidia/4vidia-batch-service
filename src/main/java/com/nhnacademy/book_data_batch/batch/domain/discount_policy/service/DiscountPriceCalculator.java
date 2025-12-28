package com.nhnacademy.book_data_batch.batch.domain.discount_policy.service;

import com.nhnacademy.book_data_batch.domain.DiscountPolicy;

public class DiscountPriceCalculator {

    /**
     * 정가와 할인율로 할인가 계산 (내림).
     * @param priceStandard 정가
     * @param discountRate 할인율(0~100)
     * @return 할인가 (null if priceStandard is null)
     */
    public Integer calculate(Integer priceStandard, int discountRate) {
        if (priceStandard == null) {
            return null;
        }
        int rate = Math.min(Math.max(discountRate, 0), 100);
        return (int) Math.floor(priceStandard * (100 - rate) / 100.0);
    }

    /** 기본 10% 할인 */
    public Integer calculateDefault(Integer priceStandard) {
        return calculate(priceStandard, 10);
    }

    public Integer calculate(Integer priceStandard, DiscountPolicy policy) {
        int rate = policy != null && policy.getDiscountRate() != null
                ? policy.getDiscountRate()
                : 10;
        return calculate(priceStandard, rate);
    }
}
