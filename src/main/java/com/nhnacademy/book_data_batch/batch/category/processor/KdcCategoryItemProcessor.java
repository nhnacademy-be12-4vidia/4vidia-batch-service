package com.nhnacademy.book_data_batch.batch.category.processor;

import com.nhnacademy.book_data_batch.batch.category.dto.KdcCategoryCsv;
import com.nhnacademy.book_data_batch.entity.Category;
import com.nhnacademy.book_data_batch.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.util.StringUtils;

@Slf4j
@RequiredArgsConstructor
public class KdcCategoryItemProcessor implements ItemProcessor<KdcCategoryCsv, Category> {

    private final CategoryRepository categoryRepository;
    private final KdcCategoryDepth targetDepth;

    // CSV 한 줄을 읽어 지정된 깊이에 해당하는 Category 엔티티로 변환
    @Override
    public Category process(KdcCategoryCsv item) {

        // 빈 항목 처리
        if (item == null || item.isEmpty()) {
            return null;
        }

        // KDC 코드로부터 깊이 정보 추출
        String rawCode = item.rawCode();
        KdcCategoryDepth depth = KdcCategoryDepth.fromCode(rawCode);

        // 목표 깊이가 아니면 건너뜀 (주류 -> 강목 -> 요목 순)
        if (depth != targetDepth) {
            if (log.isTraceEnabled()) {
                log.trace("목표 깊이가 아니라서 건너뜀 - target={}, actual={}, code={}", targetDepth, depth, rawCode);
            }
            return null;
        }

        // 카테고리 정보 생성
        String codeForStorage = rawCode;
        String name = StringUtils.hasText(item.rawName()) ? item.rawName().trim() : "미정";

        // 이미 존재하는 카테고리면 건너뜀
        if (categoryRepository.existsByKdcCode(codeForStorage)) {
            if (log.isDebugEnabled()) {
                log.debug("이미 등록된 카테고리라 생략 - code={}", codeForStorage);
            }
            return null;
        }

        // 상위 카테고리 조회 및 경로 생성
        Category parent = depth.resolveParent(categoryRepository, rawCode);
        String path = depth.buildPath(parent, rawCode);

        return Category.builder()
            .parentCategory(parent)
            .kdcCode(codeForStorage)
            .name(name)
            .path(path)
            .depth(depth.getLevel())
            .build();
    }

}
