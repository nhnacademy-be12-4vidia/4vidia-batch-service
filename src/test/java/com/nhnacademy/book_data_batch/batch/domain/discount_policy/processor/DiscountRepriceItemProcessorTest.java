package com.nhnacademy.book_data_batch.batch.domain.discount_policy.processor;

import com.nhnacademy.book_data_batch.batch.domain.discount_policy.dto.DiscountRepriceTarget;
import com.nhnacademy.book_data_batch.batch.domain.discount_policy.service.DiscountPolicyHierarchyResolver;
import com.nhnacademy.book_data_batch.batch.domain.discount_policy.service.DiscountPriceCalculator;
import com.nhnacademy.book_data_batch.domain.Category;
import com.nhnacademy.book_data_batch.domain.DiscountPolicy;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DiscountRepriceItemProcessorTest {

    private final DiscountPolicyHierarchyResolver resolver = new DiscountPolicyHierarchyResolver();
    private final DiscountPriceCalculator calculator = new DiscountPriceCalculator();

    @Test
    void returnsDiscountedPriceWhenPolicyExistsOnCategory() throws Exception {
        Category category = categoryMock(1L, null);
        DiscountPolicy policy = policyMock(20);

        DiscountRepriceItemProcessor processor = new DiscountRepriceItemProcessor(
                Map.of(1L, policy),
                resolver,
                calculator,
                Map.of(1L, category)
        );

        DiscountRepriceTarget result = processor.process(new DiscountRepriceTarget(10L, 1000, 1000, 1L));

        assertNotNull(result);
        assertEquals(800, result.priceSales());
    }

    @Test
    void resolvesPolicyFromParentCategory() throws Exception {
        Category parent = categoryMock(10L, null);
        Category child = categoryMock(11L, parent);
        DiscountPolicy parentPolicy = policyMock(30);

        DiscountRepriceItemProcessor processor = new DiscountRepriceItemProcessor(
                Map.of(10L, parentPolicy),
                resolver,
                calculator,
                Map.of(10L, parent, 11L, child)
        );

        DiscountRepriceTarget result = processor.process(new DiscountRepriceTarget(11L, 1000, 950, 11L));

        assertNotNull(result);
        assertEquals(700, result.priceSales());
    }

    @Test
    void appliesDefaultDiscountWhenNoPolicyFound() throws Exception {
        Category category = categoryMock(20L, null);

        DiscountRepriceItemProcessor processor = new DiscountRepriceItemProcessor(
                Map.of(),
                resolver,
                calculator,
                Map.of(20L, category)
        );

        DiscountRepriceTarget result = processor.process(new DiscountRepriceTarget(12L, 1000, 1000, 20L));

        assertNotNull(result);
        assertEquals(900, result.priceSales());
    }

    @Test
    void returnsNullWhenCategoryNotFound() throws Exception {
        DiscountRepriceItemProcessor processor = new DiscountRepriceItemProcessor(
                Map.of(),
                resolver,
                calculator,
                Map.of() // intentionally empty
        );

        DiscountRepriceTarget result = processor.process(new DiscountRepriceTarget(13L, 1000, 1000, 30L));

        assertNull(result);
    }

    @Test
    void skipsWhenPriceUnchanged() throws Exception {
        Category category = categoryMock(40L, null);
        DiscountPolicy policy = policyMock(0);

        DiscountRepriceItemProcessor processor = new DiscountRepriceItemProcessor(
                Map.of(40L, policy),
                resolver,
                calculator,
                Map.of(40L, category)
        );

        DiscountRepriceTarget result = processor.process(new DiscountRepriceTarget(14L, 1000, 1000, 40L));

        assertNull(result);
    }

    private Category categoryMock(Long id, Category parent) {
        Category category = mock(Category.class);
        when(category.getId()).thenReturn(id);
        when(category.getParentCategory()).thenReturn(parent);
        return category;
    }

    private DiscountPolicy policyMock(int discountRate) {
        DiscountPolicy policy = mock(DiscountPolicy.class);
        when(policy.getDiscountRate()).thenReturn(discountRate);
        return policy;
    }
}
