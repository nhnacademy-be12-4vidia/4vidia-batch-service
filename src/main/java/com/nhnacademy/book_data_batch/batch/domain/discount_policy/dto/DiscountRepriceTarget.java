package com.nhnacademy.book_data_batch.batch.domain.discount_policy.dto;

public record DiscountRepriceTarget(
        Long bookId,
        Integer priceStandard,
        Integer priceSales,
        Long categoryId
) {
}
