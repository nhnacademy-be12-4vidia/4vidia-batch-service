package com.nhnacademy.book_data_batch.jobs.image_cleanup.step;

import com.nhnacademy.book_data_batch.jobs.image_cleanup.dto.BookDescriptionImageDto;
import com.nhnacademy.book_data_batch.jobs.image_cleanup.processor.ContentImageCleanupProcessor;
import com.nhnacademy.book_data_batch.jobs.image_cleanup.writer.ContentImageCleanupWriter;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.database.JdbcPagingItemReader;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
@RequiredArgsConstructor
public class ContentImageCleanupStepConfig {

    private static final String STEP_NAME = "contentImageCleanupStep";
    private static final int CHUNK_SIZE = 100;

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;

    private final JdbcPagingItemReader<BookDescriptionImageDto> cleanupReader;
    private final ContentImageCleanupProcessor cleanupProcessor;
    private final ContentImageCleanupWriter cleanupWriter;

    @Bean
    public Step contentImageCleanupStep() {
        return new StepBuilder(STEP_NAME, jobRepository)
                .<BookDescriptionImageDto, BookDescriptionImageDto>chunk(CHUNK_SIZE, transactionManager)
                .reader(cleanupReader)
                .processor(cleanupProcessor)
                .writer(cleanupWriter)
                .build();
    }
}
