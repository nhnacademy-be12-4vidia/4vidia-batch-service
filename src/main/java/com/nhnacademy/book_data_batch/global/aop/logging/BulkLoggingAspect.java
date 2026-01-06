package com.nhnacademy.book_data_batch.global.aop.logging;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

import java.util.Collection;

/**
 * Bulk Insert/Update 로깅 AOP
 * - RepositoryImpl의 bulk* 메서드를 대상으로 로깅
 * - 시점: 메서드 시작/완료
 * - 대상: 처리 건수, 소요 시간, 예외 발생 시 에러 메시지
 */
@Aspect
@Component
@Slf4j
public class BulkLoggingAspect {

    /**
     * Pointcut: domain.repository.impl 패키지 하위의 모든 클래스 중
     * 메서드 이름이 'bulk'로 시작하는 모든 public 메서드
     */
    @Pointcut("execution(public * com.nhnacademy.book_data_batch.domain.repository.impl..*RepositoryImpl.bulk*(..))")
    public void bulkOperation() {}

    @Around("bulkOperation()")
    public Object logBulkOperation(ProceedingJoinPoint joinPoint) throws Throwable {
        // 클래스명에서 Entity 이름 추출 (예: BookRepositoryImpl -> Book)
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String entityName = className.replace("RepositoryImpl", "").replace("Impl", "");
        
        // 메서드명 (예: bulkInsert, bulkUpdate)
        String methodName = joinPoint.getSignature().getName();
        String operation = methodName.replace("bulk", "").toUpperCase(); // INSERT, UPDATE 등

        // 인자에서 컬렉션 크기 추출 (첫 번째 인자가 컬렉션이라고 가정하거나 탐색)
        Object[] args = joinPoint.getArgs();
        int count = findCollectionSize(args);
        
        String logPrefix = String.format("[Bulk - %s-%s]", entityName, operation);
        String countStr = count >= 0 ? count + "건" : "";

        long startTime = System.currentTimeMillis();
        log.info("{} 시작 {}", logPrefix, countStr);

        try {
            Object result = joinPoint.proceed();
            long duration = System.currentTimeMillis() - startTime;
            log.info("{} 완료 {} ({}ms)", logPrefix, countStr, duration);
            return result;
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("{} 예외 발생 ({}ms) - {}", logPrefix, duration, e.getMessage());
            throw e;
        }
    }

    private int findCollectionSize(Object[] args) {
        if (args == null) return -1;
        for (Object arg : args) {
            if (arg instanceof Collection<?> collection) {
                return collection.size();
            }
        }
        return -1;
    }
}