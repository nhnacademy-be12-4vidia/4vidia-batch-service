package com.nhnacademy.book_data_batch.global.aop.logging;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

/**
 * 외부 API 호출 로깅 AOP
 * - Infrastructure layer의 Client 클래스 메서드를 대상으로 로깅
 * - 시점: API 호출 시작/완료/예외
 * - 대상: 메서드명, 소요 시간(ms), 예외 메시지
 */
@Aspect
@Component
@Slf4j
public class ApiLoggingAspect {

    @Pointcut("execution(public * com.nhnacademy.book_data_batch.infrastructure.client..*.*(..))")
    public void apiCall() {}

    @Around("apiCall()")
    public Object logApiCall(ProceedingJoinPoint joinPoint) throws Throwable {
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = joinPoint.getSignature().getName();
        String apiName = className.replace("Client", "").replace("Api", ""); // AladinApi -> Aladin

        long startTime = System.currentTimeMillis();

        try {
            Object result = joinPoint.proceed();
            long duration = System.currentTimeMillis() - startTime;
            
            if (duration > 1000) {
                log.warn("[API - {}] {} 느린 응답 ({}ms)", apiName, methodName, duration);
            } else {
                log.info("[API - {}] {} 완료 ({}ms)", apiName, methodName, duration);
            }
            
            return result;
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("[API - {}] {} 호출 중 예외 발생 ({}ms) - {}", apiName, methodName, duration, e.getMessage());
            throw e;
        }
    }
}
