package com.nhnacademy.book_data_batch.common.aop.logging;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import java.util.Collection;

/**
 * Bulk Insert 로깅 AOP
 * - Custom Repository의 bulkInsert 메서드를 대상으로 로깅
 * - 시점: 메서드 시작/완료
 * - 대상: 처리 건수, 소요 시간, 예외 발생 시 에러 메시지
 */
@Aspect
@Component
@Slf4j
public class BulkLoggingAspect {

    // 작가 Bulk Insert 로깅
    @Around("execution(* com.nhnacademy.book_data_batch.repository.bulk.impl.BulkAuthorRepositoryImpl.bulkInsert(..))")
    public Object logAuthorBulkInsert(ProceedingJoinPoint joinPoint) throws Throwable {
        Object[] args = joinPoint.getArgs();
        int count = getCollectionSize(args, 0);
        return logBulkInsert(joinPoint, "작가", count);
    }

    // 도서-작가 Bulk Insert 로깅
    @Around("execution(* com.nhnacademy.book_data_batch.repository.bulk.impl.BulkBookAuthorRepositoryImpl.bulkInsert(..))")
    public Object logBookAuthorBulkInsert(ProceedingJoinPoint joinPoint) throws Throwable {
        Object[] args = joinPoint.getArgs();
        int count = getCollectionSize(args, 0);
        return logBulkInsert(joinPoint, "도서-작가", count);
    }

    // 출판사 Bulk Insert 로깅
    @Around("execution(* com.nhnacademy.book_data_batch.repository.bulk.impl.BulkPublisherRepositoryImpl.bulkInsert(..))")
    public Object logPublisherBulkInsert(ProceedingJoinPoint joinPoint) throws Throwable {
        Object[] args = joinPoint.getArgs();
        int count = getCollectionSize(args, 0);
        return logBulkInsert(joinPoint, "출판사", count);
    }

    // 도서 이미지 Bulk Insert 로깅
    @Around("execution(* com.nhnacademy.book_data_batch.repository.bulk.impl.BulkBookImageRepositoryImpl.bulkInsert(..))")
    public Object logBookImageBulkInsert(ProceedingJoinPoint joinPoint) throws Throwable {
        Object[] args = joinPoint.getArgs();
        int count = getCollectionSize(args, 0);
        return logBulkInsert(joinPoint, "도서이미지", count);
    }

    // 태그 Bulk Insert 로깅
    @Around("execution(* com.nhnacademy.book_data_batch.repository.bulk.impl.BulkTagRepositoryImpl.bulkInsert(..))")
    public Object logTagBulkInsert(ProceedingJoinPoint joinPoint) throws Throwable {
        Object[] args = joinPoint.getArgs();
        int count = getCollectionSize(args, 0);
        return logBulkInsert(joinPoint, "태그", count);
    }

    // 도서-태그 Bulk Insert 로깅
    @Around("execution(* com.nhnacademy.book_data_batch.repository.bulk.impl.BulkBookTagRepositoryImpl.bulkInsert(..))")
    public Object logBookTagBulkInsert(ProceedingJoinPoint joinPoint) throws Throwable {
        Object[] args = joinPoint.getArgs();
        int count = getCollectionSize(args, 0);
        return logBulkInsert(joinPoint, "도서-태그", count);
    }

    // 도서 Bulk Update 로깅
    @Around("execution(* com.nhnacademy.book_data_batch.repository.bulk.impl.BulkBookRepositoryImpl.bulkUpdate(..))")
    public Object logBookBulkUpdate(ProceedingJoinPoint joinPoint) throws Throwable {
        Object[] args = joinPoint.getArgs();
        int count = getCollectionSize(args, 0);
        return logBulkInsert(joinPoint, "도서", count);
    }

    // 배치 Bulk Update 로깅
    @Around("execution(* com.nhnacademy.book_data_batch.repository.bulk.impl.BulkBatchRepositoryImpl.bulkUpdateEnrichmentStatus(..)) || " +
            "execution(* com.nhnacademy.book_data_batch.repository.bulk.impl.BulkBatchRepositoryImpl.bulkUpdateEnrichmentFailed(..))")
    public Object logBatchBulkUpdate(ProceedingJoinPoint joinPoint) throws Throwable {
        Object[] args = joinPoint.getArgs();
        int count = getCollectionSize(args, 0);
        return logBulkInsert(joinPoint, "배치", count);
    }

    // 공통 Bulk 로깅 로직
    private Object logBulkInsert(ProceedingJoinPoint joinPoint, String entityName, int count) throws Throwable {
        long startTime = System.currentTimeMillis();
        log.info("[Bulk - {}] 시작 - 총 {}건", entityName, count);

        try {
            Object result = joinPoint.proceed();
            long duration = System.currentTimeMillis() - startTime;
            log.info("[Bulk - {}] 완료 - {}건 처리 ({}ms)", entityName, count, duration);
            return result;
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("[Bulk - {}] 예외 발생 ({}ms) - {}", entityName, duration, e.getMessage());
            throw e;
        }
    }

    // 메서드 인자에서 Collection 크기 추출
    private int getCollectionSize(Object[] args, int index) {
        if (args == null || args.length <= index || args[index] == null) {
            return 0;
        }
        Object arg = args[index];
        if (arg instanceof Collection<?> collection) {
            return collection.size();
        }
        return 0;
    }
}