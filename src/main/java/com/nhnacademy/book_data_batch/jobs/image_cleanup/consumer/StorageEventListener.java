package com.nhnacademy.book_data_batch.jobs.image_cleanup.consumer;

import com.nhnacademy.book_data_batch.global.config.RabbitMQConfig;
import com.nhnacademy.book_data_batch.jobs.image_cleanup.dto.event.DescriptionImageUploadedEvent;
import com.nhnacademy.book_data_batch.domain.entity.BookDescriptionImage;
import com.nhnacademy.book_data_batch.domain.repository.BookDescriptionImageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class StorageEventListener {

    private final BookDescriptionImageRepository repository;

    @Transactional
    @RabbitListener(bindings = @QueueBinding(
        value = @Queue(value = RabbitMQConfig.STORAGE_DESCRIPTION_QUEUE, durable = "true"),
        exchange = @Exchange(value = RabbitMQConfig.STORAGE_EXCHANGE, type = "topic"),
        key = RabbitMQConfig.STORAGE_DESCRIPTION_ROUTING_KEY
    ))
    public void handleDescriptionImageUploaded(DescriptionImageUploadedEvent event) {
        log.info("Received DescriptionImageUploadedEvent: {}", event.imageUrl());
        
        if (event.imageUrl() == null || event.imageUrl().isBlank()) {
            log.warn("유효하지 않은 이미지 URL: {}", event.imageUrl());
            return; // 조용히 무시 (또는 예외 발생시켜 DLQ 보냄)
        }

        try {
            repository.save(BookDescriptionImage.builder()
                .imageUrl(event.imageUrl())
                .createdAt(event.uploadedAt())
                .build());
        } catch (DataIntegrityViolationException | IllegalArgumentException e) {
            // 데이터 문제나 제약 조건 위반은 재시도해도 실패하므로 폐기(DLQ)
            log.error("처리 불가능한 오류 (영구적): {}", e.getMessage());
            throw new AmqpRejectAndDontRequeueException("영구적 오류", e);
        } catch (Exception e) {
            // DB 연결 실패, 타임아웃 등 일시적 오류는 재시도 (RabbitMQ Requeue)
            log.error("처리 실패 (일시적 오류 예상, 재시도): {}", e.getMessage(), e);
            throw e; 
        }
    }
}
