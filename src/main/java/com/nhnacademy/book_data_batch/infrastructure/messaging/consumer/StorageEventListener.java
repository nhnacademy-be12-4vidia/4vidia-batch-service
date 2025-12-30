package com.nhnacademy.book_data_batch.infrastructure.messaging.consumer;

import com.nhnacademy.book_data_batch.batch.domain.book_image.dto.event.DescriptionImageUploadedEvent;
import com.nhnacademy.book_data_batch.domain.BookDescriptionImage;
import com.nhnacademy.book_data_batch.infrastructure.repository.BookDescriptionImageRepository;
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
        value = @Queue(value = "storage.image.uploaded.description.queue", durable = "true"),
        exchange = @Exchange(value = "storage.exchange", type = "topic"),
        key = "storage.image.uploaded.description"
    ))
    public void handleDescriptionImageUploaded(DescriptionImageUploadedEvent event) {
        log.info("Received DescriptionImageUploadedEvent: {}", event.imageUrl());

        repository.save(BookDescriptionImage.builder()
            .imageUrl(event.imageUrl())
            .createdAt(event.uploadedAt())
            .build());
    }
}
