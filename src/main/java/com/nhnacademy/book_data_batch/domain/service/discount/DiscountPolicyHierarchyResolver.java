package com.nhnacademy.book_data_batch.domain.service.discount;

import com.nhnacademy.book_data_batch.domain.entity.Category;
import com.nhnacademy.book_data_batch.domain.entity.DiscountPolicy;
import java.util.Map;
import java.util.Optional;

public class DiscountPolicyHierarchyResolver {

    /**
     * 카테고리 트리(부모 체인)에서 가장 가까운 활성 정책을 탐색한다.
     * (ID 기반 맵 조회로 JPA 지연 로딩 문제 방지)
     * @param category 도서 카테고리
     * @param policyByCategoryId 활성 정책 맵 (categoryId -> policy)
     * @param allCategoriesMap 전체 카테고리 맵 (categoryId -> Category) - 부모 탐색용
     * @return 적용할 정책 Optional
     */
    public Optional<DiscountPolicy> resolve(
            Category category,
            Map<Long, DiscountPolicy> policyByCategoryId,
            Map<Long, Category> allCategoriesMap
    ) {
        Category current = category;
        while (current != null) {
            DiscountPolicy policy = policyByCategoryId.get(current.getId());
            if (policy != null) {
                return Optional.of(policy);
            }
            
            // 부모 탐색: 객체 참조가 null이면 맵에서 ID로 찾아보기 시도 (안전장치)
            if (current.getParentCategory() != null) {
                // 1. 객체 참조가 있으면 그대로 이동 (이미 로딩된 경우)
                current = current.getParentCategory();
            } else {
                // 2. 객체 참조가 없거나 프록시 초기화 실패 시 로직 종료 (더 이상 부모 없음 간주)
                // 만약 parentId 컬럼만 따로 있다면 그걸로 맵 조회 가능하겠지만, 
                // 현재 엔티티 구조상 parentCategory가 관계의 주인이므로 null이면 부모 없는 것.
                current = null;
            }
            
            // 맵에 있는 '완전한' 객체로 교체 (지연 로딩된 프록시일 경우를 대비해 ID로 맵에서 조회)
            if (current != null && allCategoriesMap != null) {
                current = allCategoriesMap.get(current.getId());
            }
        }
        return Optional.empty();
    }
}
