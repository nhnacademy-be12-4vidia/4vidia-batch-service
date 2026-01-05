package com.nhnacademy.book_data_batch.jobs.discount_reprice.consumer;

import com.nhnacademy.book_data_batch.domain.repository.CategoryRepository;
import com.nhnacademy.book_data_batch.jobs.discount_reprice.event.DiscountPolicyChangedEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.batch.core.*;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRestartException;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

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
    void handleDiscountPolicyChanged_whenCategoryIdNull_shouldLaunchJobWithAllScope() throws Exception {
        DiscountPolicyChangedEvent event = new DiscountPolicyChangedEvent(null, "POLICY_UPDATED", LocalDateTime.now());

        consumer.handleDiscountPolicyChanged(event);

        verify(jobLauncher).run(eq(discountRepriceJob), argThat(params -> 
            params.getString("targetScope").equals("ALL") &&
            params.getLong("launchTimestamp") != null
        ));
    }

    @Test
    void handleDiscountPolicyChanged_whenCategoryIdProvided_shouldLaunchJobWithCategoryScope() throws Exception {
        Long categoryId = 123L;
        when(categoryRepository.findPathByCategoryId(categoryId))
            .thenReturn("000 > 000100 > 00010001");

        DiscountPolicyChangedEvent event = new DiscountPolicyChangedEvent(categoryId, "POLICY_UPDATED", LocalDateTime.now());

        consumer.handleDiscountPolicyChanged(event);

        verify(jobLauncher).run(eq(discountRepriceJob), argThat(params -> 
            params.getString("targetScope").equals("CATEGORY") &&
            params.getLong("categoryId").equals(categoryId) &&
            params.getString("categoryPath").equals("000 > 000100 > 00010001")
        ));
    }
}
