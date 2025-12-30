package com.nhnacademy.book_data_batch.domain.service.discount;

import com.nhnacademy.book_data_batch.domain.entity.Category;
import com.nhnacademy.book_data_batch.domain.entity.DiscountPolicy;
import java.util.Map;
import java.util.Optional;

public class DiscountPolicyHierarchyResolver {

    /**
     * 카테고리 트리(부모 체인)에서 가장 가까운 활성 정책을 탐색한다.
     * @param category 도서 카테고리
     * @param policyByCategoryId 활성 정책 맵 (categoryId -> policy)
     * @return 적용할 정책 Optional
     */
    public Optional<DiscountPolicy> resolve(Category category, Map<Long, DiscountPolicy> policyByCategoryId) {
        Category current = category;
        while (current != null) {
            DiscountPolicy policy = policyByCategoryId.get(current.getId());
            if (policy != null) {
                return Optional.of(policy);
            }
            current = current.getParentCategory();
        }
        return Optional.empty();
    }
}
