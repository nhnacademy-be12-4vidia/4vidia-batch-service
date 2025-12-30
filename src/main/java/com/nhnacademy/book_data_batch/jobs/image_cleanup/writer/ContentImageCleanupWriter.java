package com.nhnacademy.book_data_batch.jobs.image_cleanup.writer;

import com.amazonaws.services.s3.AmazonS3;
import com.nhnacademy.book_data_batch.domain.repository.BookDescriptionImageRepository;
import com.nhnacademy.book_data_batch.jobs.image_cleanup.dto.BookDescriptionImageDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

@Slf4j
@Component
@RequiredArgsConstructor
public class ContentImageCleanupWriter implements ItemWriter<BookDescriptionImageDto> {

    private final AmazonS3 amazonS3;
    private final BookDescriptionImageRepository bookDescriptionImageRepository;

    @Value("${cloud.aws.s3.bucket}")
    private String bucket;

    @Override
    public void write(Chunk<? extends BookDescriptionImageDto> items) {
        for (BookDescriptionImageDto item : items) {
            try {
                // 1. MinIO(S3)에서 파일 삭제
                String objectKey = extractObjectKey(item.imageUrl());
                amazonS3.deleteObject(bucket, objectKey);
                log.info("Deleted from S3: {}", objectKey);

                // 2. DB에서 로그 삭제
                bookDescriptionImageRepository.deleteById(item.id());
            } catch (Exception e) {
                log.error("Failed to delete image: {}", item.imageUrl(), e);
                // 여기서 예외를 던지면 트랜잭션 롤백 -> 재시도
                throw e;
            }
        }
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
