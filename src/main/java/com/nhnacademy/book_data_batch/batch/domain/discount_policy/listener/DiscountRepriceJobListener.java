package com.nhnacademy.book_data_batch.batch.domain.discount_policy.listener;

import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.batch.core.JobParameters;

/**
 * 할인 재적용 배치 잡 실행 리스너
 * rabbitmq로 잡 시작/완료/실패 알림 전송
 */
@Slf4j
@RequiredArgsConstructor
public class DiscountRepriceJobListener implements JobExecutionListener {

    private final AmqpTemplate amqpTemplate;
    private final String startRoutingKey;
    private final String completedRoutingKey;
    private final String failedRoutingKey;

    @Override
    public void beforeJob(JobExecution jobExecution) {
        log.info("[DISCOUNT-REPRICE] job start: {}", jobExecution.getJobParameters());
        amqpTemplate.convertAndSend(startRoutingKey, parameterValues(jobExecution));
    }

    @Override
    public void afterJob(JobExecution jobExecution) {
        String routingKey = jobExecution.getStatus().isUnsuccessful() ? failedRoutingKey : completedRoutingKey;
        log.info("[DISCOUNT-REPRICE] job end: status={}, params={}", jobExecution.getStatus(), jobExecution.getJobParameters());
        amqpTemplate.convertAndSend(routingKey, parameterValues(jobExecution));
    }

    private Map<String, Object> parameterValues(JobExecution jobExecution) {
        JobParameters params = jobExecution.getJobParameters();
        return params.getParameters().entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().getValue()));
    }
}
