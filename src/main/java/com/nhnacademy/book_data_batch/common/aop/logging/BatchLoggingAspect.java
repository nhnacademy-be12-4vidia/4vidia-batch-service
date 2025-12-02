package com.nhnacademy.book_data_batch.common.aop.logging;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.batch.item.Chunk;
import org.springframework.stereotype.Component;

/**
 * 배치 로깅 AOP
 * - Tasklet 로깅 (시작/완료/예외)
 * - ItemWriter 로깅
 */
@Aspect
@Component
@Slf4j
public class BatchLoggingAspect {

    /**
     * Tasklet.execute() 로깅
     */
    @Around("execution(* org.springframework.batch.core.step.tasklet.Tasklet.execute(..))")
    public Object logTasklet(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();
        String operation = joinPoint.getTarget().getClass().getSimpleName();
        
        log.info("[TASKLET] {} 시작", operation);

        try {
            Object result = joinPoint.proceed();
            long duration = System.currentTimeMillis() - startTime;
            log.info("[TASKLET] {} 완료 ({}ms)", operation, duration);
            return result;
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("[TASKLET] {} 예외 발생 ({}ms) - {}", operation, duration, e.getMessage());
            throw e;
        }
    }

    /**
     * ItemWriter.write() 로깅
     */
    @Around("execution(* org.springframework.batch.item.ItemWriter.write(..))")
    public Object logWriter(ProceedingJoinPoint joinPoint) throws Throwable {
        Object[] args = joinPoint.getArgs();
        int chunkSize = 0;
        if (args.length > 0 && args[0] instanceof Chunk<?> chunk) {
            chunkSize = chunk.size();
        }

        String className = joinPoint.getTarget().getClass().getSimpleName();
        String threadName = Thread.currentThread().getName();
        long startTime = System.currentTimeMillis();

        log.info("[WRITER - {}/{}] 시작 - {}건", className, threadName, chunkSize);

        try {
            Object result = joinPoint.proceed();
            long duration = System.currentTimeMillis() - startTime;
            log.info("[WRITER - {}/{}] 완료 - {}건 ({}ms)", className, threadName, chunkSize, duration);
            return result;
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("[WRITER - {}/{}] 예외 발생 ({}ms) - {}", className, threadName, duration, e.getMessage());
            throw e;
        }
    }
}
