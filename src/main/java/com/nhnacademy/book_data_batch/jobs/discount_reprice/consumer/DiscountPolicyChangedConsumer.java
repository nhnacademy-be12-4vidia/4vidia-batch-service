package com.nhnacademy.book_data_batch.jobs.discount_reprice.consumer;

import com.nhnacademy.book_data_batch.global.config.RabbitMQConfig;
import com.nhnacademy.book_data_batch.jobs.discount_reprice.event.DiscountPolicyChangedEvent;
import com.nhnacademy.book_data_batch.domain.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.batch.core.*;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DiscountPolicyChangedConsumer {
    
    private final JobLauncher jobLauncher;
    private final Job discountRepriceJob;
    private final CategoryRepository categoryRepository;
    
    @RabbitListener(
        queues = RabbitMQConfig.QUEUE,
        containerFactory = "rabbitListenerContainerFactory"
    )
    public void handleDiscountPolicyChanged(DiscountPolicyChangedEvent event) throws Exception {
        log.info("할인 정책 변경 이벤트 수신: categoryId={}, eventType={}", 
            event.categoryId(), event.eventType());
        
        try {
            JobParameters parameters = buildJobParameters(event);
            jobLauncher.run(discountRepriceJob, parameters);
            log.info("할인 재계산 배치 실행 완료");

        } catch (JobInstanceAlreadyCompleteException | JobParametersInvalidException | IllegalArgumentException e) {
            // 논리적 오류 또는 데이터 오류는 재시도하지 않음 (DLQ 이동 또는 폐기)
            log.error("배치 실행 불가 (재시도 안함): {}", e.getMessage());
            throw new AmqpRejectAndDontRequeueException("배치 실행 불가 오류", e);

        } catch (Exception e) {
            // 인프라 장애(DB 연결 등)나 일시적 오류는 재시도 (RabbitMQ 설정에 따름)
            log.error("배치 실행 실패 (재시도 대상): {}", e.getMessage(), e);
            throw e; 
        }
    }
    
    private JobParameters buildJobParameters(DiscountPolicyChangedEvent event) {
        JobParametersBuilder builder = new JobParametersBuilder()
            .addLong("launchTimestamp", System.currentTimeMillis());
        
        if (event.categoryId() == null) {
            builder.addString("targetScope", "ALL");
        } else {
            String categoryPath = categoryRepository.findPathByCategoryId(event.categoryId());
            if (categoryPath == null) {
                throw new IllegalArgumentException("유효하지 않은 카테고리 ID입니다: " + event.categoryId());
            }
            builder.addLong("categoryId", event.categoryId())
                  .addString("categoryPath", categoryPath)
                  .addString("targetScope", "CATEGORY");
        }
        
        return builder.toJobParameters();
    }
}
