package com.nhnacademy.book_data_batch.jobs.discount_reprice.dto;

public record DiscountRepriceTarget(
        Long bookId,
        Integer priceStandard,
        Integer priceSales,
        Long categoryId
) {
}
