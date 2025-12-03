package com.nhnacademy.book_data_batch.batch.enrichment.aladin;

import com.nhnacademy.book_data_batch.batch.enrichment.aladin.client.AladinApiClient;
import com.nhnacademy.book_data_batch.batch.enrichment.aladin.client.AladinQuotaTracker;
import com.nhnacademy.book_data_batch.batch.enrichment.aladin.mapper.AladinDataMapper;
import com.nhnacademy.book_data_batch.batch.enrichment.aladin.tasklet.AladinEnrichmentTasklet;
import com.nhnacademy.book_data_batch.repository.AuthorRepository;
import com.nhnacademy.book_data_batch.repository.BatchRepository;
import com.nhnacademy.book_data_batch.repository.BookAuthorRepository;
import com.nhnacademy.book_data_batch.repository.BookImageRepository;
import com.nhnacademy.book_data_batch.repository.BookRepository;
import com.nhnacademy.book_data_batch.repository.BookTagRepository;
import com.nhnacademy.book_data_batch.repository.TagRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.List;

/**
 * Aladin Enrichment Step 설정
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class AladinEnrichmentStepConfig {

    private static final String ALADIN_ENRICHMENT_STEP_NAME = "aladinEnrichmentStep";

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;

    // Aladin
    private final AladinApiClient aladinApiClient;
    private final AladinDataMapper aladinDataMapper;
    private final AladinQuotaTracker aladinQuotaTracker;

    // Repositories
    private final BatchRepository batchRepository;
    private final BookRepository bookRepository;
    private final AuthorRepository authorRepository;
    private final BookAuthorRepository bookAuthorRepository;
    private final TagRepository tagRepository;
    private final BookTagRepository bookTagRepository;
    private final BookImageRepository bookImageRepository;

    @Value("${aladin.api.keys}")
    private List<String> aladinApiKeys;

    @Bean
    public Step aladinEnrichmentStep() {
        return new StepBuilder(ALADIN_ENRICHMENT_STEP_NAME, jobRepository)
                .tasklet(new AladinEnrichmentTasklet(
                        batchRepository,
                        authorRepository,
                        bookAuthorRepository,
                        tagRepository,
                        bookTagRepository,
                        bookRepository,
                        bookImageRepository,
                        aladinQuotaTracker,
                        aladinApiClient,
                        aladinDataMapper,
                        aladinApiKeys
                ), transactionManager)
                .build();
    }
}
