package com.nhnacademy.book_data_batch.batch.category.processor;

import com.nhnacademy.book_data_batch.batch.category.dto.KdcCategoryCsv;
import com.nhnacademy.book_data_batch.entity.Category;
import com.nhnacademy.book_data_batch.repository.CategoryRepository;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.util.StringUtils;

@RequiredArgsConstructor
@Slf4j
public class KdcCategoryItemProcessor implements ItemProcessor<KdcCategoryCsv, Category> {

    private final CategoryRepository categoryRepository;
    private final Depth targetDepth;

    // 이미 읽은 상위 분류를 재사용하기 위한 캐시
    private final Map<String, Category> mainCategoryCache = new HashMap<>();
    private final Map<String, Category> divisionCategoryCache = new HashMap<>();

    // CSV 한 줄을 읽어 지정된 깊이에 해당하는 Category 엔티티로 변환
    @Override
    public Category process(KdcCategoryCsv item) {

        // 빈 항목 무시
        if (item == null || item.isEmpty()) {
            return null;
        }

        String rawCode = normalizeCodeLength(item.rawCode());
        if (rawCode.length() != 3 || !rawCode.chars().allMatch(Character::isDigit)) {
            return null;
        }

        Depth depth = Depth.fromCode(rawCode);

        if (depth != targetDepth) {
            if (log.isTraceEnabled()) {
                log.trace("목표 깊이가 아니라서 건너뜀 - target={}, actual={}, code={}", targetDepth, depth, rawCode);
            }
            return null;
        }
        String codeForStorage = rawCode;
        String name = StringUtils.hasText(item.rawName()) ? item.rawName().trim() : "미사용";

        if (categoryRepository.existsByKdcCode(codeForStorage)) {
            cachePersistedCategories(depth, rawCode, codeForStorage);
            if (log.isDebugEnabled()) {
                log.debug("이미 등록된 카테고리라서 생략 - code={}", codeForStorage);
            }
            return null;
        }

        Category parent = resolveParent(depth, rawCode);
        String path = buildPath(depth, rawCode, parent);

        Category category = Category.builder()
            .parentCategory(parent)
            .kdcCode(codeForStorage)
            .name(name)
            .path(path)
            .depth(depth.level)
            .build();

        updateCaches(depth, rawCode, codeForStorage, category);
        return category;
    }

    // KDC 코드를 항상 3자리 숫자로 맞춰준다.
    private String normalizeCodeLength(String rawCode) {
        if (rawCode == null) {
            return "";
        }
        String trimmed = rawCode.trim();
        if (trimmed.length() >= 3) {
            return trimmed.substring(0, 3);
        }
        return String.format("%3s", trimmed).replace(' ', '0');
    }

    // 깊이에 따라 필요한 부모 카테고리를 조회한다.
    private Category resolveParent(Depth depth, String rawCode) {
        return switch (depth) {
            case MAIN -> null;
            case DIVISION -> resolveMainCategory(getMainCode(rawCode));
            case SECTION -> resolveDivisionCategory(getDivisionCode(rawCode), rawCode);
        };
    }

    private String getMainCode(String rawCode) {
        return rawCode.substring(0, 1) + "00";
    }

    private String getDivisionCode(String rawCode) {
        return rawCode.substring(0, 2) + "0";
    }

    private Category resolveMainCategory(String key) {
        return mainCategoryCache.computeIfAbsent(key, missing ->
            categoryRepository.findByKdcCode(missing)
                .orElseThrow(() -> new IllegalStateException("상위 주류가 먼저 등록되어야 합니다. code=" + missing))
        );
    }

    private Category resolveDivisionCategory(String key, String rawCode) {
        if ("00".equals(key)) {
            return resolveMainCategory(getMainCode(rawCode));
        }

        return divisionCategoryCache.computeIfAbsent(key, missing ->
            categoryRepository.findByKdcCode(missing)
                .orElseThrow(() -> new IllegalStateException("상위 강목이 먼저 등록되어야 합니다. code=" + missing))
        );
    }

    private void updateCaches(Depth depth, String rawCode, String storedCode, Category category) {
        switch (depth) {
            case MAIN -> mainCategoryCache.putIfAbsent(storedCode, category);
            case DIVISION -> divisionCategoryCache.putIfAbsent(storedCode, category);
            default -> {
                // 세목은 부모 조회에만 의존하므로 별도 캐시가 필요하지 않음
            }
        }
    }

    private void cachePersistedCategories(Depth depth, String rawCode, String storedCode) {
        switch (depth) {
            case MAIN -> mainCategoryCache.putIfAbsent(storedCode, loadCategory(storedCode));
            case DIVISION -> divisionCategoryCache.putIfAbsent(storedCode, loadCategory(storedCode));
            case SECTION -> {
                // 세목은 즉시 저장된 부모 엔티티만 참조하면 되므로 캐시하지 않음
            }
        }
    }

    private Category loadCategory(String code) {
        return categoryRepository.findByKdcCode(code)
            .orElseThrow(() -> new IllegalStateException("기존 카테고리를 찾을 수 없습니다. code=" + code));
    }

    // depth 별로 경로를 조립하여 "주류/강목/세목" 구조를 유지한다.
    private String buildPath(Depth depth, String rawCode, Category parent) {
        return switch (depth) {
            case MAIN -> "/" + rawCode.substring(0, 1);
            case DIVISION -> appendDivisionSegment(parent.getPath(), rawCode.substring(0, 2));
            case SECTION -> {
                String basePath = appendDivisionSegment(parent.getPath(), rawCode.substring(0, 2));
                yield basePath + "/" + rawCode;
            }
        };
    }

    private String appendDivisionSegment(String parentPath, String divisionSegment) {
        if (parentPath.endsWith("/" + divisionSegment)) {
            return parentPath;
        }
        return parentPath + "/" + divisionSegment;
    }

    public enum Depth {
        MAIN(1),
        DIVISION(2),
        SECTION(3);

        private final int level;

        Depth(int level) {
            this.level = level;
        }

        static Depth fromCode(String code) {
            if (code.endsWith("00")) {
                return MAIN;
            }
            if (code.endsWith("0")) {
                return DIVISION;
            }
            return SECTION;
        }
    }
}
