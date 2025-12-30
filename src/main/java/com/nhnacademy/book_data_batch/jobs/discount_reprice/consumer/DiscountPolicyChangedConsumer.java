package com.nhnacademy.book_data_batch.jobs.discount_reprice.consumer;

import com.nhnacademy.book_data_batch.jobs.discount_reprice.event.DiscountPolicyChangedEvent;
import com.nhnacademy.book_data_batch.domain.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.batch.core.*;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRestartException;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DiscountPolicyChangedConsumer {
    
    private final JobLauncher jobLauncher;
    private final Job discountRepriceJob;
    private final CategoryRepository categoryRepository;
    
    @RabbitListener(queues = "discount.policy.reprice.queue", containerFactory = "rabbitListenerContainerFactory")
    public void handleDiscountPolicyChanged(DiscountPolicyChangedEvent event) throws
            JobExecutionAlreadyRunningException,
            JobRestartException,
            JobInstanceAlreadyCompleteException,
            JobParametersInvalidException
    {
        log.info("할인 정책 변경 이벤트 수신: categoryId={}, eventType={}", event.categoryId(), event.eventType());
        
        JobParameters parameters;
        
        if (event.categoryId() == null) {
            parameters = new JobParametersBuilder()
                .addLong("launchTimestamp", System.currentTimeMillis())
                .addString("targetScope", "ALL")
                .toJobParameters();
        } else {
            String categoryPath = categoryRepository.findPathByCategoryId(event.categoryId());
            parameters = new JobParametersBuilder()
                .addLong("launchTimestamp", System.currentTimeMillis())
                .addLong("categoryId", event.categoryId())
                .addString("categoryPath", categoryPath)
                .addString("targetScope", "CATEGORY")
                .toJobParameters();
        }
        
        jobLauncher.run(discountRepriceJob, parameters);
        log.info("할인 재계산 배치 실행 완료");
    }
}
