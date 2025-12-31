package com.nhnacademy.book_data_batch.jobs.discount_reprice.processor;

import com.nhnacademy.book_data_batch.domain.entity.Category;
import com.nhnacademy.book_data_batch.domain.entity.DiscountPolicy;
import com.nhnacademy.book_data_batch.domain.repository.CategoryRepository;
import com.nhnacademy.book_data_batch.domain.repository.DiscountPolicyRepository;
import com.nhnacademy.book_data_batch.domain.service.discount.DiscountPolicyHierarchyResolver;
import com.nhnacademy.book_data_batch.domain.service.discount.DiscountPriceCalculator;
import com.nhnacademy.book_data_batch.jobs.discount_reprice.dto.DiscountRepriceTarget;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.StepExecution;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DiscountRepriceItemProcessorTest {

    private DiscountRepriceItemProcessor processor;
    private CategoryRepository categoryRepository;
    private DiscountPolicyRepository discountPolicyRepository;
    private DiscountPolicyHierarchyResolver hierarchyResolver;
    private DiscountPriceCalculator calculator;

    @BeforeEach
    void setUp() {
        categoryRepository = mock(CategoryRepository.class);
        discountPolicyRepository = mock(DiscountPolicyRepository.class);
        hierarchyResolver = mock(DiscountPolicyHierarchyResolver.class);
        calculator = new DiscountPriceCalculator();
        
        processor = new DiscountRepriceItemProcessor(
                categoryRepository,
                discountPolicyRepository,
                hierarchyResolver,
                calculator
        );
    }

    @Test
    void process_shouldApplyDiscountCalculatedInBeforeStep() {
        // Given
        Long categoryId = 100L;
        Category category = mock(Category.class);
        when(category.getId()).thenReturn(categoryId);
        
        DiscountPolicy policy = mock(DiscountPolicy.class);
        when(policy.getDiscountRate()).thenReturn(20);

        when(categoryRepository.findAllWithParent()).thenReturn(List.of(category));
        when(discountPolicyRepository.findAllActivePolicies(any())).thenReturn(List.of());
        when(hierarchyResolver.resolve(any(), anyMap(), anyMap())).thenReturn(Optional.of(policy));

        StepExecution stepExecution = mock(StepExecution.class);
        when(stepExecution.getJobParameters()).thenReturn(new JobParameters());

        // When
        processor.beforeStep(stepExecution); // 여기서 맵 빌드
        DiscountRepriceTarget item = new DiscountRepriceTarget(1L, 10000, 10000, categoryId);
        DiscountRepriceTarget result = processor.process(item);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.priceSales()).isEqualTo(8000);
    }
}