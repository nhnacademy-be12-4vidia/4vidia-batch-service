package com.nhnacademy.book_data_batch.domain.service.discount;

import com.nhnacademy.book_data_batch.domain.entity.Category;
import com.nhnacademy.book_data_batch.domain.entity.DiscountPolicy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("DiscountPolicyHierarchyResolver 테스트")
class DiscountPolicyHierarchyResolverTest {

    private DiscountPolicyHierarchyResolver resolver;

    @BeforeEach
    void setUp() {
        resolver = new DiscountPolicyHierarchyResolver();
    }

    private Category createMockCategory(Long id, Category parentCategory) {
        Category category = mock(Category.class);
        lenient().when(category.getId()).thenReturn(id);
        lenient().when(category.getParentCategory()).thenReturn(parentCategory);
        return category;
    }

    private DiscountPolicy createMockPolicy() {
        return mock(DiscountPolicy.class);
    }

    private Map<Long, Category> createCategoryMap(Category... categories) {
        Map<Long, Category> map = new HashMap<>();
        for (Category cat : categories) {
            map.put(cat.getId(), cat);
        }
        return map;
    }

    @Test
    @DisplayName("현재 카테고리에 정책이 있으면 반환")
    void resolve_policyExistsAtCurrentCategory_returnsPolicy() {
        Category current = createMockCategory(3L, null);
        DiscountPolicy policy = createMockPolicy();

        Map<Long, DiscountPolicy> policyMap = new HashMap<>();
        policyMap.put(3L, policy);

        Map<Long, Category> categoryMap = createCategoryMap(current);

        Optional<DiscountPolicy> result = resolver.resolve(current, policyMap, categoryMap);

        assertTrue(result.isPresent());
        assertEquals(policy, result.get());
    }

    @Test
    @DisplayName("현재 카테고리에는 없고 부모 카테고리에 정책이 있음")
    void resolve_policyExistsAtParent_returnsParentPolicy() {
        Category parent = createMockCategory(2L, null);
        Category current = createMockCategory(3L, parent);
        DiscountPolicy parentPolicy = createMockPolicy();

        Map<Long, DiscountPolicy> policyMap = new HashMap<>();
        policyMap.put(2L, parentPolicy);

        Map<Long, Category> categoryMap = createCategoryMap(parent, current);

        Optional<DiscountPolicy> result = resolver.resolve(current, policyMap, categoryMap);

        assertTrue(result.isPresent());
        assertEquals(parentPolicy, result.get());
    }

    @Test
    @DisplayName("조상 카테고리 중 먼 조상의 정책이 적용됨")
    void resolve_policyExistsAtGrandparent_returnsGrandparentPolicy() {
        Category grandparent = createMockCategory(1L, null);
        Category parent = createMockCategory(2L, grandparent);
        Category current = createMockCategory(3L, parent);
        DiscountPolicy grandparentPolicy = createMockPolicy();

        Map<Long, DiscountPolicy> policyMap = new HashMap<>();
        policyMap.put(1L, grandparentPolicy);

        Map<Long, Category> categoryMap = createCategoryMap(grandparent, parent, current);

        Optional<DiscountPolicy> result = resolver.resolve(current, policyMap, categoryMap);

        assertTrue(result.isPresent());
        assertEquals(grandparentPolicy, result.get());
    }

    @Test
    @DisplayName("계층 구조에 정책이 없으면 빈 Optional 반환")
    void resolve_noPolicyInHierarchy_returnsEmpty() {
        Category grandparent = createMockCategory(1L, null);
        Category parent = createMockCategory(2L, grandparent);
        Category current = createMockCategory(3L, parent);

        Map<Long, DiscountPolicy> policyMap = new HashMap<>();
        Map<Long, Category> categoryMap = createCategoryMap(grandparent, parent, current);

        Optional<DiscountPolicy> result = resolver.resolve(current, policyMap, categoryMap);

        assertFalse(result.isPresent());
    }

    @Test
    @DisplayName("null 카테고리 입력 시 빈 Optional 반환")
    void resolve_nullCategory_returnsEmpty() {
        Map<Long, DiscountPolicy> policyMap = new HashMap<>();
        Map<Long, Category> categoryMap = new HashMap<>();

        Optional<DiscountPolicy> result = resolver.resolve(null, policyMap, categoryMap);

        assertFalse(result.isPresent());
    }

    @Test
    @DisplayName("null 정책 맵 입력 시 예외 발생")
    void resolve_nullPolicyMap_throwsException() {
        Category current = createMockCategory(1L, null);

        assertThrows(NullPointerException.class, () -> {
            resolver.resolve(current, null, new HashMap<>());
        });
    }

    @Test
    @DisplayName("null 카테고리 맵 입력 시에도 정책 조회 가능")
    void resolve_nullCategoryMap_stillFindsPolicy() {
        Category current = createMockCategory(2L, null);
        DiscountPolicy policy = createMockPolicy();

        Map<Long, DiscountPolicy> policyMap = new HashMap<>();
        policyMap.put(2L, policy);

        Optional<DiscountPolicy> result = resolver.resolve(current, policyMap, null);

        assertTrue(result.isPresent());
        assertEquals(policy, result.get());
    }

    @Test
    @DisplayName("부모가 null인 단일 카테고리에서 정책 찾기")
    void resolve_categoryWithNullParent_findsPolicyIfExists() {
        Category current = createMockCategory(1L, null);
        DiscountPolicy policy = createMockPolicy();

        Map<Long, DiscountPolicy> policyMap = new HashMap<>();
        policyMap.put(1L, policy);

        Map<Long, Category> categoryMap = createCategoryMap(current);

        Optional<DiscountPolicy> result = resolver.resolve(current, policyMap, categoryMap);

        assertTrue(result.isPresent());
        assertEquals(policy, result.get());
    }

    @Test
    @DisplayName("여러层级 중 가장 가까운 정책 적용")
    void resolve_multiplePoliciesAvailable_returnsNearestPolicy() {
        Category grandparent = createMockCategory(1L, null);
        Category parent = createMockCategory(2L, grandparent);
        Category current = createMockCategory(3L, parent);
        DiscountPolicy grandparentPolicy = createMockPolicy();
        DiscountPolicy parentPolicy = createMockPolicy();

        Map<Long, DiscountPolicy> policyMap = new HashMap<>();
        policyMap.put(1L, grandparentPolicy);
        policyMap.put(2L, parentPolicy);

        Map<Long, Category> categoryMap = createCategoryMap(grandparent, parent, current);

        Optional<DiscountPolicy> result = resolver.resolve(current, policyMap, categoryMap);

        assertTrue(result.isPresent());
        assertEquals(parentPolicy, result.get());
    }

    @Test
    @DisplayName("allCategoriesMap으로 완전한 객체로 교체하여 탐색")
    void resolve_withAllCategoriesMap_replacesWithCompleteObject() {
        Category greatGrandparent = createMockCategory(0L, null);
        Category grandparent = createMockCategory(1L, greatGrandparent);
        Category parent = createMockCategory(2L, grandparent);
        Category current = createMockCategory(3L, parent);
        DiscountPolicy grandparentPolicy = createMockPolicy();

        Map<Long, DiscountPolicy> policyMap = new HashMap<>();
        policyMap.put(1L, grandparentPolicy);

        Map<Long, Category> categoryMap = createCategoryMap(greatGrandparent, grandparent, parent, current);

        Optional<DiscountPolicy> result = resolver.resolve(current, policyMap, categoryMap);

        assertTrue(result.isPresent());
        assertEquals(grandparentPolicy, result.get());
    }

    @Test
    @DisplayName("정책 맵이 비어있으면 빈 Optional 반환")
    void resolve_emptyPolicyMap_returnsEmpty() {
        Category current = createMockCategory(1L, null);

        Map<Long, DiscountPolicy> policyMap = new HashMap<>();
        Map<Long, Category> categoryMap = createCategoryMap(current);

        Optional<DiscountPolicy> result = resolver.resolve(current, policyMap, categoryMap);

        assertFalse(result.isPresent());
    }

    @Test
    @DisplayName("깊은 카테고리 계층에서 올바르게 정책 탐색")
    void resolve_deepHierarchy_findsPolicyAtDeepLevel() {
        Category level4 = createMockCategory(4L, null);
        Category level3 = createMockCategory(3L, level4);
        Category level2 = createMockCategory(2L, level3);
        Category level1 = createMockCategory(1L, level2);
        DiscountPolicy level3Policy = createMockPolicy();

        Map<Long, DiscountPolicy> policyMap = new HashMap<>();
        policyMap.put(3L, level3Policy);

        Map<Long, Category> categoryMap = createCategoryMap(level4, level3, level2, level1);

        Optional<DiscountPolicy> result = resolver.resolve(level1, policyMap, categoryMap);

        assertTrue(result.isPresent());
        assertEquals(level3Policy, result.get());
    }
}
