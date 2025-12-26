package com.nhnacademy.book_data_batch.batch.domain.discount_policy.processor;

import com.nhnacademy.book_data_batch.batch.domain.discount_policy.dto.DiscountRepriceTarget;
import com.nhnacademy.book_data_batch.batch.domain.discount_policy.service.DiscountPolicyHierarchyResolver;
import com.nhnacademy.book_data_batch.batch.domain.discount_policy.service.DiscountPriceCalculator;
import com.nhnacademy.book_data_batch.domain.Category;
import com.nhnacademy.book_data_batch.domain.DiscountPolicy;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.item.ItemProcessor;

/**
 * 카테고리 할인 정책에 맞게 도서 할인가 계산
 */
@RequiredArgsConstructor
public class DiscountRepriceItemProcessor implements ItemProcessor<DiscountRepriceTarget, DiscountRepriceTarget> {

    private final Map<Long, DiscountPolicy> policyByCategoryId;
    private final DiscountPolicyHierarchyResolver resolver;
    private final DiscountPriceCalculator calculator;
    private final Map<Long, Category> categoriesById;

    @Override
    public DiscountRepriceTarget process(DiscountRepriceTarget item) {
        Category category = categoriesById.get(item.categoryId());
        if (category == null) {
            return null;
        }
        Optional<DiscountPolicy> policy = resolver.resolve(category, policyByCategoryId);
        Integer newPrice = policy
                .map(p -> calculator.calculate(item.priceStandard(), p.getDiscountRate()))
                .orElseGet(() -> calculator.calculateDefault(item.priceStandard()));

        if (newPrice == null || newPrice.equals(item.priceSales())) {
            return null;
        }

        return new DiscountRepriceTarget(item.bookId(), item.priceStandard(), newPrice, item.categoryId());
    }
}
