package com.nhnacademy.book_data_batch.infrastructure.messaging.consumer;

import com.nhnacademy.book_data_batch.global.config.RabbitMQConfig;
import com.nhnacademy.book_data_batch.jobs.image_cleanup.dto.event.DescriptionImageUploadedEvent;
import com.nhnacademy.book_data_batch.domain.entity.BookDescriptionImage;
import com.nhnacademy.book_data_batch.domain.repository.BookDescriptionImageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
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

        repository.save(BookDescriptionImage.builder()
            .imageUrl(event.imageUrl())
            .createdAt(event.uploadedAt())
            .build());
    }
}
