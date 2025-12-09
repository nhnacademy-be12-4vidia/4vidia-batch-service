package com.nhnacademy.book_data_batch.batch.category.tasklet;

import com.nhnacademy.book_data_batch.domain.Category;
import com.nhnacademy.book_data_batch.infrastructure.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;

@Slf4j
@RequiredArgsConstructor
public class NonKdcCategoryTasklet implements Tasklet {

    private final CategoryRepository categoryRepository;

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {

        if (categoryRepository.existsByKdcCode("UNC")) {
            return RepeatStatus.FINISHED;
        }

        categoryRepository.save(createUncategorizedCategory());
        log.info("미분류 카테고리 생성 완료");

        return RepeatStatus.FINISHED;
    }

    private Category createUncategorizedCategory() {
        return Category.builder()
                .parentCategory(null)
                .kdcCode("UNC")
                .name("미분류")
                .path("/UNC")
                .depth(1)
                .build();
    }
}
