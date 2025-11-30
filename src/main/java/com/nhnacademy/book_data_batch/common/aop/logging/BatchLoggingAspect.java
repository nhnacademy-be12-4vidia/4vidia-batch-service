package com.nhnacademy.book_data_batch.common.aop.logging;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.batch.item.Chunk;
import org.springframework.stereotype.Component;

@Aspect
@Component
@Slf4j
public class BatchLoggingAspect {

    // Tasklet
    @Around("execution(* org.springframework.batch.core.step.tasklet.Tasklet.execute(..))")
    public Object logTasklet(ProceedingJoinPoint joinPoint) throws Throwable {long startTime = System.currentTimeMillis();

        String operation = joinPoint.getTarget().getClass().getSimpleName();
        log.info("[TASKLET] {} 시작", operation);

        try {
            Object result = joinPoint.proceed();
            long endTime = System.currentTimeMillis();
            log.info("[TASKLET] {} 완료 ({}ms)",
                    operation, (endTime - startTime));
            return result;
        } catch (Exception e) {
            long errorTime = System.currentTimeMillis();
            log.error("[TASKLET] {} 예외 발생 ({}ms) - {}",
                    operation, (errorTime - startTime), e.getMessage());
            throw e;
        }
    }

    // Chunk-oriented Step
    // Writer
    @Around("execution(* org.springframework.batch.item.ItemWriter.write(..))")
    public Object logWriter(ProceedingJoinPoint joinPoint) throws Throwable {

        // 청크 크기 추출
        Object[] args = joinPoint.getArgs();
        int chunkSize = 0;
        if (args.length > 0 && args[0] instanceof Chunk<?> chunk) {
            chunkSize = chunk.size();
        }

        // 클래스 이름 추출
        String className = joinPoint.getTarget().getClass().getSimpleName();

        // 스레드 이름 추출
        String threadName = Thread.currentThread().getName();

        // 시간 측정 시작
        long start = System.currentTimeMillis();
        log.info("[CHUNK - {}/{}] 시작 - {}건",
                className, threadName, chunkSize);

        try {
            Object result = joinPoint.proceed();
            log.info("[CHUNK - {}/{}] 완료 - {}건 ({}ms)",
                    className, threadName, System.currentTimeMillis() - start, chunkSize);
            return result;
        } catch (Exception e) {
            log.error("[CHUNK - {}/{}] 예외 발생 ({}ms) - {}",
                    className, threadName, System.currentTimeMillis() - start, e.getMessage());
            throw e;
        }
    }
}
