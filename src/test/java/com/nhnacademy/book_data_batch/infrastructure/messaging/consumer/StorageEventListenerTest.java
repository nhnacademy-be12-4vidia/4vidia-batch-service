package com.nhnacademy.book_data_batch.infrastructure.messaging.consumer;

import com.nhnacademy.book_data_batch.domain.entity.BookDescriptionImage;
import com.nhnacademy.book_data_batch.domain.repository.BookDescriptionImageRepository;
import com.nhnacademy.book_data_batch.jobs.image_cleanup.dto.event.DescriptionImageUploadedEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class StorageEventListenerTest {

    @Mock
    private BookDescriptionImageRepository repository;

    @InjectMocks
    private StorageEventListener listener;

    @Test
    void handleDescriptionImageUploaded_whenCalled_shouldSaveRecord() {
        DescriptionImageUploadedEvent event = new DescriptionImageUploadedEvent(
            "https://example.com/image.jpg",
            LocalDateTime.now()
        );

        listener.handleDescriptionImageUploaded(event);

        verify(repository).save(argThat(image -> 
            image.getImageUrl().equals("https://example.com/image.jpg") &&
            image.getCreatedAt() != null
        ));
    }
}
