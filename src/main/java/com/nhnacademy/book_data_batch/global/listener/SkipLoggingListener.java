package com.nhnacademy.book_data_batch.global.listener;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.SkipListener;
import org.springframework.stereotype.Component;

/**
 * 배치 처리 중 Skip 발생 시 로그를 남기는 리스너
 */
@Slf4j
@Component
public class SkipLoggingListener implements SkipListener<Object, Object> {

    @Override
    public void onSkipInRead(Throwable t) {
        log.warn("[SKIP-READ] 읽기 중 스킵 발생: {}", t.getMessage());
    }

    @Override
    public void onSkipInWrite(Object item, Throwable t) {
        log.warn("[SKIP-WRITE] 쓰기 중 스킵 발생 - Item: {}, Error: {}", item, t.getMessage());
    }

    @Override
    public void onSkipInProcess(Object item, Throwable t) {
        log.warn("[SKIP-PROCESS] 처리 중 스킵 발생 - Item: {}, Error: {}", item, t.getMessage());
    }
}
