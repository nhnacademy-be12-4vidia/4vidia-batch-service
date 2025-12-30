package com.nhnacademy.book_data_batch.jobs.image_cleanup.config;

import com.amazonaws.services.s3.AmazonS3;
import com.nhnacademy.book_data_batch.domain.entity.Book;
import com.nhnacademy.book_data_batch.domain.entity.BookDescriptionImage;
import com.nhnacademy.book_data_batch.infrastructure.jdbc.JdbcExecutor;
import com.nhnacademy.book_data_batch.domain.repository.BookDescriptionImageRepository;
import com.nhnacademy.book_data_batch.domain.repository.BookRepository;
import com.nhnacademy.book_data_batch.jobs.image_cleanup.processor.ContentImageCleanupProcessor;
import com.nhnacademy.book_data_batch.jobs.image_cleanup.reader.ContentImageCleanupReaderConfig;
import com.nhnacademy.book_data_batch.jobs.image_cleanup.step.ContentImageCleanupStepConfig;
import com.nhnacademy.book_data_batch.jobs.image_cleanup.writer.ContentImageCleanupWriter;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@SpringBatchTest
@SpringBootTest(classes = {
    ContentImageCleanupJobConfig.class,
    ContentImageCleanupStepConfig.class,
    ContentImageCleanupReaderConfig.class,
    ContentImageCleanupProcessor.class,
    ContentImageCleanupWriter.class,
    ContentImageCleanupJobConfigTest.TestConfig.class,
    JdbcExecutor.class
})
@EnableAutoConfiguration
@ActiveProfiles("test")
class ContentImageCleanupJobConfigTest {

    @Autowired
    private JobLauncherTestUtils jobLauncherTestUtils;

    @Autowired
    private BookDescriptionImageRepository bookDescriptionImageRepository;

    @Autowired
    private BookRepository bookRepository;

    @MockitoBean
    private AmazonS3 amazonS3;

    @Autowired
    private Job contentImageCleanupJob;

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
        bookDescriptionImageRepository.deleteAll();
        bookRepository.deleteAll();
    }

    @Test
    @DisplayName("고아 이미지 정리 배치 테스트: 사용 중인 이미지는 유지하고, 미사용 이미지는 삭제")
    void contentImageCleanupJob_deletesUnusedImages() throws Exception {
        // Given
        // 설정된 bucket 이름(test-bucket)과 일치하는 경로 사용
        String usedImageUrl = "http://storage/test-bucket/used.jpg";
        String unusedImageUrl = "http://storage/test-bucket/unused.jpg"; 
        String recentImageUrl = "http://storage/test-bucket/recent.jpg";

        // 1. 이미지 로그 데이터 셋팅
        BookDescriptionImage usedImage = BookDescriptionImage.builder()
                .imageUrl(usedImageUrl)
                .createdAt(LocalDateTime.now().minusHours(25)) // 25시간 전 (대상)
                .build();

        BookDescriptionImage unusedImage = BookDescriptionImage.builder()
                .imageUrl(unusedImageUrl)
                .createdAt(LocalDateTime.now().minusHours(25)) // 25시간 전 (대상)
                .build();

        BookDescriptionImage recentImage = BookDescriptionImage.builder()
                .imageUrl(recentImageUrl)
                .createdAt(LocalDateTime.now().minusHours(1)) // 1시간 전 (비대상)
                .build();

        bookDescriptionImageRepository.saveAll(List.of(usedImage, unusedImage, recentImage));

        // 2. 도서 데이터 셋팅 (usedImageUrl만 사용함)
        Book book = Book.builder()
                .title("Test Book")
                .isbn("1234567890123")
                .description("이 책은 " + usedImageUrl + " 를 포함하고 있습니다.")
                .publishedDate(LocalDate.now())
                .publisher(null) // Publisher는 테스트용으로 null
                .build();
        
        bookRepository.save(book);


        // When
        jobLauncherTestUtils.setJob(contentImageCleanupJob);
        JobParameters jobParameters = new JobParametersBuilder()
                .addLong("time", System.currentTimeMillis())
                .toJobParameters();

        JobExecution jobExecution = jobLauncherTestUtils.launchJob(jobParameters);

        // Then
        // 1. Job 성공 확인
        assertThat(jobExecution.getStatus().isUnsuccessful()).isFalse();

        // 2. DB 데이터 검증
        List<BookDescriptionImage> remainingImages = bookDescriptionImageRepository.findAll();
        
        // usedImage(사용중)와 recentImage(최신)는 남아있어야 함
        assertThat(remainingImages).hasSize(2);
        assertThat(remainingImages).extracting("imageUrl")
                .containsExactlyInAnyOrder(usedImageUrl, recentImageUrl);
        
        // unusedImage는 삭제되어야 함
        assertThat(remainingImages).extracting("imageUrl")
                .doesNotContain(unusedImageUrl);

        // 3. S3 삭제 호출 검증 (정확한 버킷명과 키 추출 검증)
        // URL: http://storage/test-bucket/unused.jpg -> Key: unused.jpg
        verify(amazonS3).deleteObject("test-bucket", "unused.jpg");
    }
}