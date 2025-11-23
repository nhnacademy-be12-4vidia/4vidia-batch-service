package com.nhnacademy.book_data_batch.batch.category;

import com.nhnacademy.book_data_batch.batch.category.dto.KdcCategoryCsv;
import com.nhnacademy.book_data_batch.batch.category.mapper.KdcCategoryLineMapper;
import com.nhnacademy.book_data_batch.batch.category.processor.KdcCategoryItemProcessor;
import com.nhnacademy.book_data_batch.entity.Category;
import com.nhnacademy.book_data_batch.repository.CategoryRepository;
import jakarta.persistence.EntityManagerFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.database.JpaItemWriter;
import org.springframework.batch.item.database.builder.JpaItemWriterBuilder;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
@RequiredArgsConstructor
public class KdcCategoryJobConfig {

    private static final String JOB_NAME = "kdcCategoryJob";
    private static final String MAIN_STEP_NAME = "kdcMainCategoryStep";
    private static final String DIVISION_STEP_NAME = "kdcDivisionCategoryStep";
    private static final String SECTION_STEP_NAME = "kdcSectionCategoryStep";
    private static final int CHUNK_SIZE = 100;

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final EntityManagerFactory entityManagerFactory;
    private final CategoryRepository categoryRepository;

    @Bean
    public Job kdcCategoryJob(Step kdcMainCategoryStep,
                              Step kdcDivisionCategoryStep,
                              Step kdcSectionCategoryStep) {

        // KDC 는 주류 → 강목 → 요목 순서를 보장해야 하므로 세 개의 Step 을 직렬로 배치
        return new JobBuilder(JOB_NAME, jobRepository)
            .start(kdcMainCategoryStep) // 주류
            .next(kdcDivisionCategoryStep) // 강목
            .next(kdcSectionCategoryStep) // 요목
            .preventRestart() // 실패 시 재시작 불가
            .build();
    }

    // Step: 주류(depth 1) 처리
    @Bean
    public Step kdcMainCategoryStep(
        @Qualifier("kdcCategoryReader") FlatFileItemReader<KdcCategoryCsv> kdcMainCategoryReader,
        @Qualifier("kdcMainCategoryProcessor") ItemProcessor<KdcCategoryCsv, Category> kdcMainCategoryProcessor,
        JpaItemWriter<Category> kdcCategoryWriter) {
        return buildStep(MAIN_STEP_NAME, kdcMainCategoryReader, kdcMainCategoryProcessor, kdcCategoryWriter);
    }

    // Step: 강목(depth 2) 처리
    @Bean
    public Step kdcDivisionCategoryStep(
        @Qualifier("kdcCategoryReader") FlatFileItemReader<KdcCategoryCsv> kdcDivisionCategoryReader,
        @Qualifier("kdcDivisionCategoryProcessor") ItemProcessor<KdcCategoryCsv, Category> kdcDivisionCategoryProcessor,
        JpaItemWriter<Category> kdcCategoryWriter) {
        return buildStep(DIVISION_STEP_NAME, kdcDivisionCategoryReader, kdcDivisionCategoryProcessor, kdcCategoryWriter);
    }

    // Step: 요목(depth 3) 처리
    @Bean
    public Step kdcSectionCategoryStep(
        @Qualifier("kdcCategoryReader") FlatFileItemReader<KdcCategoryCsv> kdcSectionCategoryReader,
        @Qualifier("kdcSectionCategoryProcessor") ItemProcessor<KdcCategoryCsv, Category> kdcSectionCategoryProcessor,
        JpaItemWriter<Category> kdcCategoryWriter) {
        return buildStep(SECTION_STEP_NAME, kdcSectionCategoryReader, kdcSectionCategoryProcessor, kdcCategoryWriter);
    }

    // Reader: KDC 카테고리 CSV 파일 읽기
    @Bean
    @StepScope
    public FlatFileItemReader<KdcCategoryCsv> kdcCategoryReader(
        @Value("${batch.kdc-category.resource:classpath:data/kdc_table.csv}") Resource resource) {
        return createReader(resource, "kdcCategoryReader");
    }

    // Processor: 주류(depth 1) 카테고리 처리
    @Bean
    @StepScope
    public ItemProcessor<KdcCategoryCsv, Category> kdcMainCategoryProcessor() {
        return new KdcCategoryItemProcessor(categoryRepository, KdcCategoryItemProcessor.Depth.MAIN);
    }

    // Processor: 강목(depth 2) 카테고리 처리
    @Bean
    @StepScope
    public ItemProcessor<KdcCategoryCsv, Category> kdcDivisionCategoryProcessor() {
        return new KdcCategoryItemProcessor(categoryRepository, KdcCategoryItemProcessor.Depth.DIVISION);
    }

    // Processor: 요목(depth 3) 카테고리 처리
    @Bean
    @StepScope
    public ItemProcessor<KdcCategoryCsv, Category> kdcSectionCategoryProcessor() {
        return new KdcCategoryItemProcessor(categoryRepository, KdcCategoryItemProcessor.Depth.SECTION);
    }

    // Writer: JPA 를 사용하여 Category 엔티티 저장
    @Bean
    public JpaItemWriter<Category> kdcCategoryWriter() {
        return new JpaItemWriterBuilder<Category>()
            .entityManagerFactory(entityManagerFactory)
            .build();
    }

    // Step 빌더 메서드
    private Step buildStep(String stepName,
                           FlatFileItemReader<KdcCategoryCsv> reader,
                           ItemProcessor<KdcCategoryCsv, Category> processor,
                           JpaItemWriter<Category> writer) {
        // 동일한 CSV 에 대해 세 번 반복해서 읽기 때문에 StepScope 빈을 활용하고, 고정 청크 크기를 사용한다.
        return new StepBuilder(stepName, jobRepository)
            .<KdcCategoryCsv, Category>chunk(CHUNK_SIZE, transactionManager)
            .reader(reader)
            .processor(processor)
            .writer(writer)
            .build();
    }

    // Reader 생성 메서드
    private FlatFileItemReader<KdcCategoryCsv> createReader(Resource resource, String name) {
        // 동일한 라인 매퍼를 공유하지만 Step 마다 이름을 달리하여 상태 구분을 명확히 한다.
        return new FlatFileItemReaderBuilder<KdcCategoryCsv>()
            .name(name)
            .resource(resource)
            .lineMapper(new KdcCategoryLineMapper())
            .strict(true)
            .build();
    }
}
