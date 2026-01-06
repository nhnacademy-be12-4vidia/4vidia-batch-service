package com.nhnacademy.book_data_batch.jobs.discount_reprice.consumer;

import com.nhnacademy.book_data_batch.domain.repository.CategoryRepository;
import com.nhnacademy.book_data_batch.jobs.discount_reprice.event.DiscountPolicyChangedEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParametersInvalidException;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRestartException;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DiscountPolicyChangedConsumerTest {

    @Mock
    private JobLauncher jobLauncher;

    @Mock
    private Job discountRepriceJob;

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private DiscountPolicyChangedConsumer consumer;

    @Test
    @DisplayName("카테고리 ID가 없으면 전체(ALL) 범위로 배치를 실행한다")
    void handleDiscountPolicyChanged_whenCategoryIdNull_shouldLaunchJobWithAllScope() throws Exception {
        // given
        DiscountPolicyChangedEvent event = new DiscountPolicyChangedEvent(null, "POLICY_UPDATED", LocalDateTime.now());

        // when
        consumer.handleDiscountPolicyChanged(event);

        // then
        verify(jobLauncher).run(eq(discountRepriceJob), argThat(params -> 
            "ALL".equals(params.getString("targetScope")) &&
            params.getLong("launchTimestamp") != null
        ));
    }

    @Test
    @DisplayName("카테고리 ID가 있으면 해당 카테고리(CATEGORY) 범위로 배치를 실행한다")
    void handleDiscountPolicyChanged_whenCategoryIdProvided_shouldLaunchJobWithCategoryScope() throws Exception {
        // given
        Long categoryId = 123L;
        String categoryPath = "/8/81/813";
        
        when(categoryRepository.findPathByCategoryId(categoryId))
            .thenReturn(categoryPath);

        DiscountPolicyChangedEvent event = new DiscountPolicyChangedEvent(categoryId, "POLICY_UPDATED", LocalDateTime.now());

        // when
        consumer.handleDiscountPolicyChanged(event);

        // then
        verify(jobLauncher).run(eq(discountRepriceJob), argThat(params -> 
            "CATEGORY".equals(params.getString("targetScope")) &&
            categoryId.equals(params.getLong("categoryId")) &&
            categoryPath.equals(params.getString("categoryPath"))
        ));
    }

    @Test
    @DisplayName("배치 파라미터가 유효하지 않으면 예외를 잡아서 메시지를 폐기(Reject)한다")
    void handleDiscountPolicyChanged_whenInvalidParameters_shouldReject() throws Exception {
        // given
        DiscountPolicyChangedEvent event = new DiscountPolicyChangedEvent(null, "POLICY_UPDATED", LocalDateTime.now());

        when(jobLauncher.run(any(), any()))
            .thenThrow(new JobParametersInvalidException("Invalid parameters"));

        // when & then
        assertThrows(AmqpRejectAndDontRequeueException.class, 
            () -> consumer.handleDiscountPolicyChanged(event));
    }

    @Test
    @DisplayName("이미 완료된 배치 인스턴스인 경우 예외를 잡아서 메시지를 폐기(Reject)한다")
    void handleDiscountPolicyChanged_whenJobAlreadyComplete_shouldReject() throws Exception {
        // given
        DiscountPolicyChangedEvent event = new DiscountPolicyChangedEvent(null, "POLICY_UPDATED", LocalDateTime.now());

        when(jobLauncher.run(any(), any()))
            .thenThrow(new JobInstanceAlreadyCompleteException("Already complete"));

        // when & then
        assertThrows(AmqpRejectAndDontRequeueException.class, 
            () -> consumer.handleDiscountPolicyChanged(event));
    }

    @Test
    @DisplayName("알 수 없는 오류 발생 시 예외를 다시 던져 재시도 유도")
    void handleDiscountPolicyChanged_whenUnknownError_shouldRethrow() throws Exception {
        // Given
        DiscountPolicyChangedEvent event = new DiscountPolicyChangedEvent(null, "POLICY_UPDATED", LocalDateTime.now());
        
        doThrow(new RuntimeException("Unexpected error"))
                .when(jobLauncher).run(any(), any());

        // When & Then
        assertThrows(RuntimeException.class, () -> 
            consumer.handleDiscountPolicyChanged(event)
        );
        
        verify(jobLauncher).run(any(), any());
    }
}
