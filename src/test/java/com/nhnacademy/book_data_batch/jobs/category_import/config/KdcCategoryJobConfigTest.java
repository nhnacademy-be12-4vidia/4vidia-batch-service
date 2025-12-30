package com.nhnacademy.book_data_batch.jobs.category_import.config;

import com.nhnacademy.book_data_batch.domain.entity.Category;
import com.nhnacademy.book_data_batch.domain.repository.CategoryRepository;
import com.nhnacademy.book_data_batch.infrastructure.jdbc.JdbcExecutor;
import com.nhnacademy.book_data_batch.jobs.category_import.mapper.KdcCategoryLineMapper;
import com.nhnacademy.book_data_batch.jobs.category_import.processor.KdcCategoryItemProcessor;
import com.nhnacademy.book_data_batch.jobs.category_import.tasklet.NonKdcCategoryTasklet;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.*;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBatchTest
@SpringBootTest(classes = {
    KdcCategoryJobConfig.class,
    KdcCategoryJobConfigTest.TestConfig.class,
    JdbcExecutor.class
})
@EnableAutoConfiguration
@ActiveProfiles("test")
@TestPropertySource(properties = "batch.kdc-category.resource=classpath:data/kdc_test.csv")
class KdcCategoryJobConfigTest {

    @Autowired
    private JobLauncherTestUtils jobLauncherTestUtils;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private Job kdcCategoryJob;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @TestConfiguration
    @EnableJpaRepositories(basePackages = "com.nhnacademy.book_data_batch.domain.repository")
    @EntityScan(basePackages = "com.nhnacademy.book_data_batch.domain")
    @ComponentScan(basePackages = "com.nhnacademy.book_data_batch.domain.repository.impl")
    @EnableTransactionManagement
    @EnableJpaAuditing
    static class TestConfig {
    }

    @AfterEach
    void tearDown() {
        categoryRepository.deleteAll();
    }

    @Test
    @DisplayName("KDC 카테고리 임포트 배치 테스트: 계층 구조 생성 확인")
    void kdcCategoryJob_importsCategoriesCorrectly() throws Exception {
        // Given
        // 테스트용 CSV 파일은 src/test/resources/data/kdc_test.csv 에 위치해야 함
        // 내용은 다음과 같다고 가정:
        // 000,총류
        // 010,도서학
        // 011,저작권

        // When
        jobLauncherTestUtils.setJob(kdcCategoryJob);
        JobParameters jobParameters = new JobParametersBuilder()
                .addLong("time", System.currentTimeMillis())
                .toJobParameters();

        JobExecution jobExecution = jobLauncherTestUtils.launchJob(jobParameters);

        // Then
        assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);

        // 트랜잭션 내에서 검증 (Lazy Loading 문제 해결)
        new TransactionTemplate(transactionManager).execute(status -> {
            // 1. 주류 (000) 확인
            Category main = categoryRepository.findByKdcCode("000").orElseThrow();
            assertThat(main.getCategoryName()).isEqualTo("총류");
            assertThat(main.getDepth()).isEqualTo(1);
            assertThat(main.getParentCategory()).isNull();
            assertThat(main.getPath()).isEqualTo("/0");

            // 2. 강목 (010) 확인
            Category division = categoryRepository.findByKdcCode("010").orElseThrow();
            assertThat(division.getCategoryName()).isEqualTo("도서학");
            assertThat(division.getDepth()).isEqualTo(2);
            assertThat(division.getParentCategory()).isEqualTo(main);
            assertThat(division.getPath()).isEqualTo("/0/01");

            // 3. 요목 (011) 확인
            Category section = categoryRepository.findByKdcCode("011").orElseThrow();
            assertThat(section.getCategoryName()).isEqualTo("저작");
            assertThat(section.getDepth()).isEqualTo(3);
            assertThat(section.getParentCategory()).isEqualTo(division);
            assertThat(section.getPath()).isEqualTo("/0/01/011");
            return null;
        });
    }
}
