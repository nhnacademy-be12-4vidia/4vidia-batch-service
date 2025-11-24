package com.nhnacademy.book_data_batch.batch.category.processor;

import com.nhnacademy.book_data_batch.entity.Category;
import com.nhnacademy.book_data_batch.repository.CategoryRepository;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;
import java.util.Optional;

/**
 * KDC 코드에 따른 깊이(Root → Division → Section)와 부가 정보를 계산하는 헬퍼.
 */
@Getter
@RequiredArgsConstructor
public enum KdcCategoryDepth {

    MAIN(1) {
        @Override
        public boolean matches(String code) {
            return code.endsWith("00");
        }

        @Override
        public Optional<String> parentCode(String code) {
            return Optional.empty();
        }

        @Override
        public String buildPath(Category parent, String code) {
            return "/" + code.substring(0, 1);
        }
    },

    DIVISION(2) {
        @Override
        public boolean matches(String code) {
            return code.endsWith("0") && !code.endsWith("00");
        }

        @Override
        public Optional<String> parentCode(String code) {
            return Optional.of(code.substring(0, 1) + "00");
        }

        @Override
        public String buildPath(Category parent, String code) {
            if (parent == null) {
                throw new IllegalStateException("강목의 상위 주류가 존재해야 합니다. code=" + code);
            }
            return parent.getPath() + "/" + code.substring(0, 2);
        }
    },

    SECTION(3) {
        @Override
        public boolean matches(String code) {
            return !code.endsWith("0");
        }

        @Override
        public Optional<String> parentCode(String code) {
            return Optional.of(code.substring(0, 2) + "0");
        }

        @Override
        public String buildPath(Category parent, String code) {
            if (parent == null) {
                throw new IllegalStateException("세목의 상위 강목이 존재해야 합니다. code=" + code);
            }
            return parent.getPath() + "/" + code;
        }
    };

    private final int level;

    public abstract boolean matches(String code);
    public abstract Optional<String> parentCode(String code);
    public abstract String buildPath(Category parent, String code);

    // KDC -> Depth 매핑
    public static KdcCategoryDepth fromCode(String normalizedCode) {
        return Arrays.stream(values())
            .filter(depth -> depth.matches(normalizedCode))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("지원하지 않는 KDC 코드입니다. code=" + normalizedCode));
    }

    // 상위 카테고리 조회
    public Category resolveParent(CategoryRepository repository, String normalizedCode) {
        return parentCode(normalizedCode)
            .map(parentCode -> repository.findByKdcCode(parentCode)
                .orElseThrow(() -> new IllegalStateException("상위 카테고리가 먼저 등록되어야 합니다. code=" + parentCode)))
            .orElse(null);
    }
}
