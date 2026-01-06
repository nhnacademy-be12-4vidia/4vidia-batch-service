package com.nhnacademy.book_data_batch.jobs.image_cleanup.consumer;

import com.nhnacademy.book_data_batch.global.config.RabbitMQConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class StorageDescriptionDlqConsumer {

    @RabbitListener(queues = RabbitMQConfig.STORAGE_DESCRIPTION_DLQ)
    public void handleDlqMessage(Message message) {
        String messageBody = new String(message.getBody());
        
        log.error("🚨 DLQ 메시지 수신 - URL: {}, Headers: {}", 
            messageBody, message.getMessageProperties().getHeaders());
        
        log.warn("DLQ 메시지가 저장되었습니다. 관리자 검토 필요.");
        
        // TODO: 모니터링 & 주기적 정리 작업 스케줄링
    }
}
