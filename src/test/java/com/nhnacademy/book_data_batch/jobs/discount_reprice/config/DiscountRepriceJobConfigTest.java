package com.nhnacademy.book_data_batch.jobs.discount_reprice.config;

import com.nhnacademy.book_data_batch.domain.entity.Book;
import com.nhnacademy.book_data_batch.domain.entity.Category;
import com.nhnacademy.book_data_batch.domain.entity.DiscountPolicy;
import com.nhnacademy.book_data_batch.domain.repository.BookRepository;
import com.nhnacademy.book_data_batch.domain.repository.CategoryRepository;
import com.nhnacademy.book_data_batch.domain.repository.DiscountPolicyRepository;
import com.nhnacademy.book_data_batch.infrastructure.jdbc.JdbcExecutor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
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
import org.springframework.transaction.annotation.EnableTransactionManagement;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.junit.jupiter.api.BeforeEach;

@SpringBatchTest
@SpringBootTest(classes = {
    DiscountRepriceJobConfig.class,
    DiscountRepriceJobConfigTest.TestConfig.class,
    JdbcExecutor.class
})
@EnableAutoConfiguration
@ActiveProfiles("test")
class DiscountRepriceJobConfigTest {

    @Autowired
    private JobLauncherTestUtils jobLauncherTestUtils;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private DiscountPolicyRepository discountPolicyRepository;

    @Autowired
    private Job discountRepriceJob;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private TransactionTemplate transactionTemplate;

    @BeforeEach
    void setUp() {
        transactionTemplate = new TransactionTemplate(transactionManager);
    }

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
        discountPolicyRepository.deleteAll();
        bookRepository.deleteAll();
        categoryRepository.deleteAll();
    }

    @Test
    @DisplayName("도서 가격 재계산 배치: 카테고리 할인 정책 적용 확인")
    void discountRepriceJob_updatesPricesBasedOnPolicy() throws Exception {
        // Given
        Long bookId = transactionTemplate.execute(status -> {
            // 1. 카테고리 계층 생성
            Category parent = Category.builder()
                    .kdcCode("100")
                    .categoryName("Parent")
                    .path("/1")
                    .depth(1)
                    .build();
            categoryRepository.save(parent);

            Category child = Category.builder()
                    .kdcCode("101")
                    .categoryName("Child")
                    .path("/1/2")
                    .depth(2)
                    .parentCategory(parent)
                    .build();
            categoryRepository.save(child);

            // 2. 할인 정책 설정 (부모 카테고리에 20% 할인)
            DiscountPolicy policy = DiscountPolicy.builder()
                    .category(parent)
                    .discountPolicyName("Parent 20% Off")
                    .discountRate(20)
                    .startDate(LocalDate.now().minusDays(1))
                    .endDate(LocalDate.now().plusDays(1))
                    .build();
            discountPolicyRepository.save(policy);

            // 3. 도서 생성 (Child 카테고리 소속, 정가 10000원)
            Book book = Book.builder()
                    .title("Test Book")
                    .isbn("9781234567890")
                    .priceStandard(10000)
                    .priceSales(10000) // 초기엔 정가와 동일하다고 가정
                    .category(child)
                    .build();
            return bookRepository.save(book).getId();
        });

        // When
        jobLauncherTestUtils.setJob(discountRepriceJob);
        JobParameters jobParameters = new JobParametersBuilder()
                .addString("targetScope", "ALL")
                .addString("asOfDate", LocalDate.now().toString())
                .addLong("time", System.currentTimeMillis())
                .toJobParameters();

        JobExecution jobExecution = jobLauncherTestUtils.launchJob(jobParameters);

        // Then
        assertThat(jobExecution.getStatus().isUnsuccessful()).isFalse();

        Book updatedBook = bookRepository.findById(bookId).orElseThrow();
        
        // 10000원 * (100-20)% = 8000원
        assertThat(updatedBook.getPriceSales()).isEqualTo(8000);
    }
}
