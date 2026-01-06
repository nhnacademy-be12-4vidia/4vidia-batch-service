package com.nhnacademy.book_data_batch.infrastructure.messaging.consumer;

import com.nhnacademy.book_data_batch.domain.entity.BookDescriptionImage;
import com.nhnacademy.book_data_batch.domain.repository.BookDescriptionImageRepository;
import com.nhnacademy.book_data_batch.jobs.image_cleanup.dto.event.DescriptionImageUploadedEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.QueryTimeoutException;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StorageEventListenerTest {

    @Mock
    private BookDescriptionImageRepository repository;

    @InjectMocks
    private StorageEventListener listener;

    @Test
    @DisplayName("성공적으로 이미지가 업로드 이벤트를 수신하면 DB에 저장한다")
    void handleDescriptionImageUploaded_whenSuccess_shouldSaveRecord() {
        // given
        DescriptionImageUploadedEvent event = new DescriptionImageUploadedEvent(
            "https://example.com/image.jpg",
            LocalDateTime.now()
        );

        when(repository.save(any(BookDescriptionImage.class)))
            .thenReturn(BookDescriptionImage.builder()
                .imageUrl(event.imageUrl())
                .createdAt(event.uploadedAt())
                .build());

        // when
        listener.handleDescriptionImageUploaded(event);

        // then
        verify(repository).save(any(BookDescriptionImage.class));
    }

    @Test
    @DisplayName("데이터 무결성 위반(중복 키 등) 발생 시 AmqpRejectAndDontRequeueException을 던져 메시지를 폐기한다")
    void handleDescriptionImageUploaded_whenDataIntegrityError_shouldRejectImmediately() {
        // given
        DescriptionImageUploadedEvent event = new DescriptionImageUploadedEvent(
            "https://example.com/image.jpg",
            LocalDateTime.now()
        );

        when(repository.save(any(BookDescriptionImage.class)))
            .thenThrow(new DataIntegrityViolationException("Duplicate key"));

        // when & then
        assertThrows(AmqpRejectAndDontRequeueException.class, 
            () -> listener.handleDescriptionImageUploaded(event));
        
        verify(repository, times(1)).save(any());
    }

    @Test
    @DisplayName("일시적인 DB 오류(TransientDataAccessException) 발생 시 예외를 그대로 던져 재시도를 유도한다")
    void handleDescriptionImageUploaded_whenTransientError_shouldRethrow() {
        // given
        DescriptionImageUploadedEvent event = new DescriptionImageUploadedEvent(
            "https://example.com/image.jpg",
            LocalDateTime.now()
        );

        // QueryTimeoutException extends TransientDataAccessException
        when(repository.save(any(BookDescriptionImage.class)))
            .thenThrow(new QueryTimeoutException("DB timeout"));

        // when & then
        assertThrows(QueryTimeoutException.class, 
            () -> listener.handleDescriptionImageUploaded(event));
        
        verify(repository, times(1)).save(any());
    }

    @Test
    @DisplayName("이미지 URL이 null이면 DB에 저장하지 않고 종료한다")
    void handleDescriptionImageUploaded_whenImageUrlNull_shouldNotSave() {
        // given
        DescriptionImageUploadedEvent event = new DescriptionImageUploadedEvent(null, LocalDateTime.now());

        // when
        listener.handleDescriptionImageUploaded(event);

        // then
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("알 수 없는 오류 발생 시 예외를 다시 던져 재시도를 유도한다")
    void handleDescriptionImageUploaded_whenUnknownError_shouldRethrow() {
        // given
        DescriptionImageUploadedEvent event = new DescriptionImageUploadedEvent(
            "https://example.com/image.jpg",
            LocalDateTime.now()
        );

        when(repository.save(any(BookDescriptionImage.class)))
            .thenThrow(new RuntimeException("Unexpected error"));

        // when & then
        // 인프라 장애 등 알 수 없는 오류는 재시도하도록 그대로 던져야 함
        assertThrows(RuntimeException.class, 
            () -> listener.handleDescriptionImageUploaded(event));
        
        verify(repository, times(1)).save(any());
    }
}