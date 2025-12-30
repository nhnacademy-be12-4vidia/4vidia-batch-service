package com.nhnacademy.book_data_batch.jobs.category_import.config;

import com.nhnacademy.book_data_batch.jobs.category_import.dto.KdcCategoryCsv;
import com.nhnacademy.book_data_batch.jobs.category_import.mapper.KdcCategoryLineMapper;
import com.nhnacademy.book_data_batch.jobs.category_import.processor.KdcCategoryDepth;
import com.nhnacademy.book_data_batch.jobs.category_import.processor.KdcCategoryItemProcessor;
import com.nhnacademy.book_data_batch.jobs.category_import.tasklet.NonKdcCategoryTasklet;
import com.nhnacademy.book_data_batch.domain.entity.Category;
import com.nhnacademy.book_data_batch.domain.repository.CategoryRepository;
import jakarta.persistence.EntityManagerFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.tasklet.Tasklet;
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
    private static final String NON_KDC_STEP_NAME = "nonKdcCategoryStep";
    private static final int CHUNK_SIZE = 20;

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
                .next(nonKdcCategoryStep()) // KDC 이외의 카테고리
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

    @Bean
    public Step nonKdcCategoryStep() {
        return new StepBuilder(NON_KDC_STEP_NAME, jobRepository)
            .tasklet(new NonKdcCategoryTasklet(categoryRepository), transactionManager)
            .build();
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
        return new KdcCategoryItemProcessor(categoryRepository, KdcCategoryDepth.MAIN);
    }

    // Processor: 강목(depth 2) 카테고리 처리
    @Bean
    @StepScope
    public ItemProcessor<KdcCategoryCsv, Category> kdcDivisionCategoryProcessor() {
        return new KdcCategoryItemProcessor(categoryRepository, KdcCategoryDepth.DIVISION);
    }

    // Processor: 요목(depth 3) 카테고리 처리
    @Bean
    @StepScope
    public ItemProcessor<KdcCategoryCsv, Category> kdcSectionCategoryProcessor() {
        return new KdcCategoryItemProcessor(categoryRepository, KdcCategoryDepth.SECTION);
    }

    // Writer: JPA 를 사용하여 Category 엔티티 저장
    @Bean
    public JpaItemWriter<Category> kdcCategoryWriter() {
        return new JpaItemWriterBuilder<Category>()
            .entityManagerFactory(entityManagerFactory)
            .build();
    }

    @Bean
    public Tasklet nonKdcCategoryTasklet() {
        return new NonKdcCategoryTasklet(categoryRepository);
    }

    // Step 빌더 메서드
    private Step buildStep(String stepName,
                           FlatFileItemReader<KdcCategoryCsv> reader,
                           ItemProcessor<KdcCategoryCsv, Category> processor,
                           JpaItemWriter<Category> writer) {
        // 동일한 CSV 에 대해 세 번 반복해서 읽기 때문에 StepScope 빈을 활용하고, 고정 청크 크기 사용
        // 각 Step 별로 Reader, Processor, Writer 를 주입받아 구성
        // Reader 와 Processor 는 StepScope 로 정의되어 있어 Step 실행 시점에 생성됨
        // Writer 는 공통으로 사용
        // *StepScope: Step 실행 시점에 빈이 생성되어 Step 별로 독립적인 상태를 유지할 수 있음
        // *고정 청크 크기: 메모리 사용량과 성능 간의 균형을 맞추기 위해 적절한 크기를 설정
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
        // strict(true): 파일이 없거나 읽을 수 없을 때 예외를 발생시켜 문제를 조기에 발견할 수 있도록 함
        // 상태 구분: 동일한 CSV 파일을 세 번 읽기 때문에 Reader 이름을 다르게 하여 Step 별로 구분 (로그에서 식별용)
        return new FlatFileItemReaderBuilder<KdcCategoryCsv>()
            .name(name)
            .resource(resource)
            .lineMapper(new KdcCategoryLineMapper())
            .strict(true)
            .build();
    }
}
