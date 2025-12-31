package com.nhnacademy.book_data_batch.jobs.discount_reprice.processor;

import com.nhnacademy.book_data_batch.domain.entity.Category;
import com.nhnacademy.book_data_batch.domain.entity.DiscountPolicy;
import com.nhnacademy.book_data_batch.domain.repository.CategoryRepository;
import com.nhnacademy.book_data_batch.domain.repository.DiscountPolicyRepository;
import com.nhnacademy.book_data_batch.domain.service.discount.DiscountPolicyHierarchyResolver;
import com.nhnacademy.book_data_batch.domain.service.discount.DiscountPriceCalculator;
import com.nhnacademy.book_data_batch.jobs.discount_reprice.dto.DiscountRepriceTarget;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.item.ItemProcessor;

/**
 * 카테고리 할인 정책에 맞게 도서 할인가 계산
 * (성능 최적화: Step 시작 시 모든 카테고리 할인율을 미리 계산함)
 */
@Slf4j
@RequiredArgsConstructor
public class DiscountRepriceItemProcessor implements ItemProcessor<DiscountRepriceTarget, DiscountRepriceTarget>, StepExecutionListener {

    private final CategoryRepository categoryRepository;
    private final DiscountPolicyRepository discountPolicyRepository;
    private final DiscountPolicyHierarchyResolver hierarchyResolver;
    private final DiscountPriceCalculator calculator;

    private Map<Long, Integer> discountRateMap;
    private static final int DEFAULT_DISCOUNT_RATE = 10;

    @Override
    public void beforeStep(StepExecution stepExecution) {
        log.info("Step 시작 전: 모든 카테고리에 대한 할인율 미리 계산 중...");

        // JobParameter에서 날짜 가져오기 (없으면 오늘)
        String asOfDateStr = stepExecution.getJobParameters().getString("asOfDate");
        LocalDate asOfDate = asOfDateStr != null ? LocalDate.parse(asOfDateStr) : LocalDate.now();

        // 1. 모든 카테고리 및 활성 정책 로드
        List<Category> allCategories = categoryRepository.findAllWithParent();
        List<DiscountPolicy> activePolicies = discountPolicyRepository.findAllActivePolicies(asOfDate);

        // 2. 카테고리 ID 맵 생성 (탐색 안전성 확보)
        Map<Long, Category> allCategoriesMap = allCategories.stream()
                .collect(Collectors.toMap(Category::getId, Function.identity()));

        // 3. 정책 맵핑 (CategoryId -> Policy) 및 전역 정책 식별
        Map<Long, DiscountPolicy> policyMap = new HashMap<>();
        DiscountPolicy globalPolicy = null;

        for (DiscountPolicy p : activePolicies) {
            if (p.getCategory() != null) {
                policyMap.put(p.getCategory().getId(), p);
            } else {
                globalPolicy = p;
            }
        }

        // 4. 각 카테고리별 최종 할인율 계산
        this.discountRateMap = new HashMap<>();
        for (Category category : allCategories) {
            Optional<DiscountPolicy> appliedPolicy = hierarchyResolver.resolve(category, policyMap, allCategoriesMap);

            Integer rate;
            if (appliedPolicy.isPresent()) {
                rate = appliedPolicy.get().getDiscountRate();
            } else if (globalPolicy != null) {
                rate = globalPolicy.getDiscountRate();
            } else {
                rate = DEFAULT_DISCOUNT_RATE;
            }
            this.discountRateMap.put(category.getId(), rate);
        }

        log.info("할인율 맵 생성 완료. 대상 카테고리 수: {}, 적용 기준일: {}", discountRateMap.size(), asOfDate);
    }

    @Override
    public DiscountRepriceTarget process(DiscountRepriceTarget item) {
        if (discountRateMap == null || !discountRateMap.containsKey(item.categoryId())) {
            return null;
        }

        int rate = discountRateMap.get(item.categoryId());
        Integer newPrice = calculator.calculate(item.priceStandard(), rate);

        if (newPrice == null || newPrice.equals(item.priceSales())) {
            return null;
        }

        return new DiscountRepriceTarget(item.bookId(), item.priceStandard(), newPrice, item.categoryId());
    }

    @Override
    public ExitStatus afterStep(StepExecution stepExecution) {
        return null;
    }
}
