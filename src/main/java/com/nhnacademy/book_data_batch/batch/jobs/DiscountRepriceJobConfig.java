package com.nhnacademy.book_data_batch.batch.jobs;

import com.nhnacademy.book_data_batch.batch.domain.discount_policy.dto.DiscountRepriceTarget;
import com.nhnacademy.book_data_batch.batch.domain.discount_policy.processor.DiscountRepriceItemProcessor;
import com.nhnacademy.book_data_batch.batch.domain.discount_policy.reader.DiscountRepriceItemReader;
import com.nhnacademy.book_data_batch.batch.domain.discount_policy.service.DiscountPolicyHierarchyResolver;
import com.nhnacademy.book_data_batch.batch.domain.discount_policy.service.DiscountPriceCalculator;
import com.nhnacademy.book_data_batch.batch.domain.discount_policy.listener.DiscountRepriceJobListener;
import com.nhnacademy.book_data_batch.batch.domain.discount_policy.writer.DiscountRepriceItemWriter;
import com.nhnacademy.book_data_batch.domain.Category;
import com.nhnacademy.book_data_batch.domain.DiscountPolicy;
import com.nhnacademy.book_data_batch.infrastructure.jdbc.JdbcExecutor;
import com.nhnacademy.book_data_batch.infrastructure.repository.CategoryRepository;
import com.nhnacademy.book_data_batch.infrastructure.repository.DiscountPolicyRepository;
import jakarta.persistence.EntityManagerFactory;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
@RequiredArgsConstructor
public class DiscountRepriceJobConfig {

    private static final String JOB_NAME = "discountRepriceJob";
    private static final String STEP_NAME = "discountRepriceStep";
    private static final int CHUNK_SIZE = 500;

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final EntityManagerFactory entityManagerFactory;
    private final CategoryRepository categoryRepository;
    private final DiscountPolicyRepository discountPolicyRepository;
    private final JdbcExecutor jdbcExecutor;
    private final AmqpTemplate amqpTemplate;

    @Bean
    public Job discountRepriceJob(Step discountRepriceStep,
                                  DiscountRepriceJobListener discountRepriceJobListener) {
        return new JobBuilder(JOB_NAME, jobRepository)
                .listener(discountRepriceJobListener)
                .start(discountRepriceStep)
                .build();
    }

    @Bean
    public Step discountRepriceStep(
            ItemReader<DiscountRepriceTarget> discountRepriceReader,
            ItemProcessor<DiscountRepriceTarget, DiscountRepriceTarget> discountRepriceProcessor,
            ItemWriter<DiscountRepriceTarget> discountRepriceWriter) {
        return new StepBuilder(STEP_NAME, jobRepository)
                .<DiscountRepriceTarget, DiscountRepriceTarget>chunk(CHUNK_SIZE, transactionManager)
                .reader(discountRepriceReader)
                .processor(discountRepriceProcessor)
                .writer(discountRepriceWriter)
                .build();
    }

    @Bean
    @StepScope
    public ItemReader<DiscountRepriceTarget> discountRepriceReader(
            @Value("#{jobParameters['policyCategoryPath']}") String policyCategoryPath,
            @Value("#{jobParameters['policyCategoryId']}") Long policyCategoryId,
            @Value("#{jobParameters['asOfDate']}") String asOfDateStr) {
        LocalDate asOfDate = asOfDateStr != null ? LocalDate.parse(asOfDateStr) : LocalDate.now();
        List<Long> descendantIds = categoryRepository.findDescendantIdsByPathPrefix(policyCategoryPath);
        List<Long> activePolicyCategoryIds = discountPolicyRepository.findActivePolicies(descendantIds, asOfDate)
                .stream()
                .map(dp -> dp.getCategory().getId())
                .filter(id -> !id.equals(policyCategoryId))
                .toList();

        String query = "SELECT new com.nhnacademy.book_data_batch.batch.domain.discount_policy.dto.DiscountRepriceTarget(" +
                " b.id, b.priceStandard, b.priceSales, c.id " +
                ") FROM Book b JOIN b.category c " +
                "WHERE c.path LIKE :pathPrefix " +
                "AND b.category.id NOT IN :excludedCategoryIds";
        Map<String, Object> params = Map.of(
                "pathPrefix", policyCategoryPath + "%",
                "excludedCategoryIds", activePolicyCategoryIds
        );
        return new DiscountRepriceItemReader(entityManagerFactory, query, params, CHUNK_SIZE);
    }

    @Bean
    @StepScope
    public ItemProcessor<DiscountRepriceTarget, DiscountRepriceTarget> discountRepriceProcessor(
            @Value("#{jobParameters['policyCategoryPath']}") String policyCategoryPath,
            @Value("#{jobParameters['asOfDate']}") String asOfDateStr) {
        List<Long> descendantIds = categoryRepository.findDescendantIdsByPathPrefix(policyCategoryPath);
        LocalDate asOfDate = asOfDateStr != null ? LocalDate.parse(asOfDateStr) : LocalDate.now();

        Map<Long, DiscountPolicy> policyMap = discountPolicyRepository.findActivePolicies(descendantIds, asOfDate)
                .stream()
                .collect(Collectors.toMap(dp -> dp.getCategory().getId(), Function.identity()));

        List<Category> categories = categoryRepository.findDescendantsByPathPrefix(policyCategoryPath);
        Map<Long, Category> categoriesById = categories.stream()
                .collect(Collectors.toMap(Category::getId, Function.identity()));

        return new DiscountRepriceItemProcessor(
                policyMap,
                new DiscountPolicyHierarchyResolver(),
                new DiscountPriceCalculator(),
                categoriesById
        );
    }

    @Bean
    public ItemWriter<DiscountRepriceTarget> discountRepriceWriter() {
        return new DiscountRepriceItemWriter(jdbcExecutor);
    }

    @Bean
    public DiscountRepriceJobListener discountRepriceJobListener(
            @Value("${discount.reprice.event.started:discount.reprice.started}") String startedRoutingKey,
            @Value("${discount.reprice.event.completed:discount.reprice.completed}") String completedRoutingKey,
            @Value("${discount.reprice.event.failed:discount.reprice.failed}") String failedRoutingKey) {
        return new DiscountRepriceJobListener(amqpTemplate, startedRoutingKey, completedRoutingKey, failedRoutingKey);
    }
}
