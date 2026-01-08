package com.nhnacademy.book_data_batch.jobs.discount_reprice.config;

import com.nhnacademy.book_data_batch.domain.service.discount.DiscountPolicyHierarchyResolver;
import com.nhnacademy.book_data_batch.domain.service.discount.DiscountPriceCalculator;
import com.nhnacademy.book_data_batch.jobs.discount_reprice.dto.DiscountRepriceTarget;
import com.nhnacademy.book_data_batch.jobs.discount_reprice.processor.DiscountRepriceItemProcessor;
import com.nhnacademy.book_data_batch.jobs.discount_reprice.reader.DiscountRepriceItemReader;
import com.nhnacademy.book_data_batch.jobs.discount_reprice.writer.DiscountRepriceItemWriter;
import com.nhnacademy.book_data_batch.domain.repository.CategoryRepository;
import com.nhnacademy.book_data_batch.domain.repository.DiscountPolicyRepository;
import com.nhnacademy.book_data_batch.infrastructure.jdbc.JdbcExecutor;
import com.nhnacademy.book_data_batch.jobs.discount_reprice.writer.EsPriceUpdateWriter;
import jakarta.persistence.EntityManagerFactory;

import java.util.List;
import java.util.Map;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.support.builder.CompositeItemWriterBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.dao.DataAccessResourceFailureException;
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
    private final JdbcExecutor jdbcExecutor;

    @Bean
    public Job discountRepriceJob(Step discountRepriceStep) {
        return new JobBuilder(JOB_NAME, jobRepository)
                .start(discountRepriceStep)
                .build();
    }

    @Bean
    public Step discountRepriceStep(
            ItemReader<DiscountRepriceTarget> discountRepriceReader,
            DiscountRepriceItemProcessor discountRepriceProcessor,
            ItemWriter<DiscountRepriceTarget> discountRepriceWriter
    ) {
        return new StepBuilder(STEP_NAME, jobRepository)
                .<DiscountRepriceTarget, DiscountRepriceTarget>chunk(CHUNK_SIZE, transactionManager)
                .listener(discountRepriceProcessor) // Processor가 Listener 역할도 수행
                .reader(discountRepriceReader)
                .processor(discountRepriceProcessor)
                .writer(discountRepriceWriter)
                .faultTolerant()
                .retryLimit(3)
                .retry(SocketTimeoutException.class)
                .retry(ConnectException.class)
                .retry(DataAccessResourceFailureException.class)
                .build();
    }

    @Bean
    @StepScope
    public DiscountRepriceItemReader discountRepriceReader(
            @Value("#{jobParameters['targetScope']}") String targetScope,
            @Value("#{jobParameters['categoryPath']}") String categoryPath
    ) {
        String query;
        Map<String, Object> params;

        if ("ALL".equals(targetScope)) {
            query = "SELECT new com.nhnacademy.book_data_batch.jobs.discount_reprice.dto.DiscountRepriceTarget(" +
                    " b.id, b.priceStandard, b.priceSales, c.id " +
                    ") FROM Book b JOIN b.category c " +
                    "ORDER BY b.id";
            params = Map.of();
        } else {
            query = "SELECT new com.nhnacademy.book_data_batch.jobs.discount_reprice.dto.DiscountRepriceTarget(" +
                    " b.id, b.priceStandard, b.priceSales, c.id " +
                    ") FROM Book b JOIN b.category c " +
                    "WHERE c.path LIKE :pathPrefix " +
                    "ORDER BY b.id";
            params = Map.of("pathPrefix", categoryPath + "%");
        }

        return new DiscountRepriceItemReader(entityManagerFactory, query, params, CHUNK_SIZE);
    }

    @Bean
    @StepScope
    public DiscountRepriceItemProcessor discountRepriceProcessor(
            CategoryRepository categoryRepository,
            DiscountPolicyRepository discountPolicyRepository,
            DiscountPolicyHierarchyResolver hierarchyResolver,
            DiscountPriceCalculator discountPriceCalculator
    ) {
        return new DiscountRepriceItemProcessor(
                categoryRepository,
                discountPolicyRepository,
                hierarchyResolver,
                discountPriceCalculator
        );
    }

    @Bean
    public ItemWriter<DiscountRepriceTarget> discountRepriceWriter(
            ElasticsearchOperations elasticsearchOperations,
            @Value("${spring.elasticsearch.index.book}") String bookIndexName
    ) {
        // 1. DB 업데이트 Writer
        DiscountRepriceItemWriter dbWriter = new DiscountRepriceItemWriter(jdbcExecutor);

        // 2. ES 업데이트 Writer
        EsPriceUpdateWriter esWriter = new EsPriceUpdateWriter(elasticsearchOperations, bookIndexName);

        // 3. CompositeItemWriter로 묶어서 반환 (순차 실행)
        return new CompositeItemWriterBuilder<DiscountRepriceTarget>()
                .delegates(List.of(dbWriter, esWriter))
                .build();
    }
    
}
