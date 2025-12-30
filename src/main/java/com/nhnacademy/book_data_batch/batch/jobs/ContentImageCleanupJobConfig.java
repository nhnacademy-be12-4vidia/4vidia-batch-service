package com.nhnacademy.book_data_batch.batch.jobs;

import com.amazonaws.services.s3.AmazonS3;
import com.nhnacademy.book_data_batch.infrastructure.repository.BookDescriptionImageRepository;
import com.nhnacademy.book_data_batch.infrastructure.repository.BookRepository;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.JdbcPagingItemReader;
import org.springframework.batch.item.database.Order;
import org.springframework.batch.item.database.PagingQueryProvider;
import org.springframework.batch.item.database.builder.JdbcPagingItemReaderBuilder;
import org.springframework.batch.item.database.support.SqlPagingQueryProviderFactoryBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.batch.core.configuration.annotation.StepScope;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class ContentImageCleanupJobConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final DataSource dataSource;
    private final BookRepository bookRepository;
    private final BookDescriptionImageRepository bookDescriptionImageRepository;
    private final AmazonS3 amazonS3;

    @Value("${cloud.aws.s3.bucket}")
    private String bucket;

    private static final int CHUNK_SIZE = 100;

    @Getter
    @Setter
    @NoArgsConstructor
    public static class BookDescriptionImageDto {
        private Long id;
        private String imageUrl;
        private LocalDateTime createdAt;
    }

    @Bean
    public Job contentImageCleanupJob() throws Exception {
        return new JobBuilder("contentImageCleanupJob", jobRepository)
                .start(contentImageCleanupStep())
                .build();
    }

    @Bean
    public Step contentImageCleanupStep() throws Exception {
        return new StepBuilder("contentImageCleanupStep", jobRepository)
                .<BookDescriptionImageDto, BookDescriptionImageDto>chunk(CHUNK_SIZE, transactionManager)
                .reader(cleanupReader())
                .processor(cleanupProcessor())
                .writer(cleanupWriter())
                .build();
    }

    @Bean
    public JdbcPagingItemReader<BookDescriptionImageDto> cleanupReader() throws Exception {
        Map<String, Order> sortKeys = new HashMap<>();
        sortKeys.put("id", Order.ASCENDING);

        // 24시간 지난 이미지 조회
        LocalDateTime oneDayAgo = LocalDateTime.now().minusDays(1);

        return new JdbcPagingItemReaderBuilder<BookDescriptionImageDto>()
                .name("cleanupReader")
                .dataSource(dataSource)
                .fetchSize(CHUNK_SIZE)
                .rowMapper(new BeanPropertyRowMapper<>(BookDescriptionImageDto.class))
                .queryProvider(cleanupQueryProvider())
                .parameterValues(Map.of("oneDayAgo", oneDayAgo))
                .build();
    }

    @Bean
    public PagingQueryProvider cleanupQueryProvider() throws Exception {
        SqlPagingQueryProviderFactoryBean queryProvider = new SqlPagingQueryProviderFactoryBean();
        queryProvider.setDataSource(dataSource);
        queryProvider.setSelectClause("id, image_url, created_at");
        queryProvider.setFromClause("from book_description_image");
        queryProvider.setWhereClause("created_at < :oneDayAgo");
        queryProvider.setSortKeys(Map.of("id", Order.ASCENDING));
        return queryProvider.getObject();
    }

    @Bean
    @StepScope // Step 실행 시마다 새로 생성되어야 함 (상태 저장)
    public ItemProcessor<BookDescriptionImageDto, BookDescriptionImageDto> cleanupProcessor() {
        // 1. 메모리 최적화: 최근 수정된 도서의 Description을 미리 로딩
        // (배치 시작 기준 24시간 이내 수정된 책들에는, 24시간 전 업로드된 이미지가 있을 수 있음)
        // 안전하게 48시간(2일) 전 수정본까지 로딩
        LocalDate twoDaysAgo = LocalDate.now().minusDays(2);
        List<String> recentDescriptions = bookRepository.findDescriptionsByUpdatedAtAfter(twoDaysAgo);
        
        log.info("Pre-loaded {} book descriptions for optimization.", recentDescriptions.size());

        return item -> {
            String imageUrl = item.getImageUrl();
            
            // 2. 메모리 상에서 매칭
            // Description 리스트를 순회하며 해당 URL이 포함되어 있는지 확인
            boolean matchRaw = recentDescriptions.stream()
                    .anyMatch(desc -> desc.contains(imageUrl));
            
            boolean matchEncoded = false;
            if (!matchRaw) {
                try {
                    String encodedUrl = URLEncoder.encode(imageUrl, StandardCharsets.UTF_8);
                    matchEncoded = recentDescriptions.stream()
                            .anyMatch(desc -> desc.contains(encodedUrl));
                } catch (Exception e) {
                    log.warn("URL Encoding failed for: {}", imageUrl, e);
                }
            }

            if (matchRaw || matchEncoded) {
                // 사용 중이면 Writer로 넘기지 않음 (null 반환 = Filter)
                return null;
            }
            
            // 미사용이면 삭제 대상이므로 Writer로 전달
            log.info("Deleting unused image: {}", imageUrl);
            return item;
        };
    }

    @Bean
    public ItemWriter<BookDescriptionImageDto> cleanupWriter() {
        return items -> {
            for (BookDescriptionImageDto item : items) {
                try {
                    // 1. MinIO(S3)에서 파일 삭제
                    String objectKey = extractObjectKey(item.getImageUrl());
                    amazonS3.deleteObject(bucket, objectKey);
                    log.info("Deleted from S3: {}", objectKey);

                    // 2. DB에서 로그 삭제
                    bookDescriptionImageRepository.deleteById(item.getId());
                } catch (Exception e) {
                    log.error("Failed to delete image: {}", item.getImageUrl(), e);
                    // 여기서 예외를 던지면 트랜잭션 롤백 -> 재시도
                    throw e; 
                }
            }
        };
    }

    private String extractObjectKey(String imageUrl) {
        try {
            URI uri = new URI(imageUrl);
            String path = uri.getPath();
            String decodedPath = URLDecoder.decode(path, StandardCharsets.UTF_8);
            String bucketPrefix = "/" + bucket + "/";

            if (decodedPath.startsWith(bucketPrefix)) {
                return decodedPath.substring(bucketPrefix.length());
            } else {
                 if (decodedPath.startsWith("/")) {
                    return decodedPath.substring(1);
                }
                return decodedPath;
            }
        } catch (Exception e) {
            log.warn("Failed to extract key from URL: {}", imageUrl, e);
            return imageUrl; // Fallback
        }
    }
}