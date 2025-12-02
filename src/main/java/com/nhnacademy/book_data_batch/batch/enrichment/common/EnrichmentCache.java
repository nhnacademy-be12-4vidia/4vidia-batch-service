package com.nhnacademy.book_data_batch.batch.enrichment.common;

import com.nhnacademy.book_data_batch.batch.enrichment.aladin.dto.AladinEnrichmentData;
import com.nhnacademy.book_data_batch.batch.enrichment.aladin.dto.BookEnrichmentTarget;
import com.nhnacademy.book_data_batch.batch.enrichment.aladin.dto.FailedEnrichment;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Thread-safe한 Enrichment 결과 수집기
 * 
 * <p>Step 간 데이터 공유를 위한 캐시</p>
 * <ul>
 *   <li>Step 1: PENDING 도서 목록 저장</li>
 *   <li>Step 2: API 호출 결과 수집 (성공/실패)</li>
 *   <li>Step 3: 결과 조회 후 DB 저장</li>
 * </ul>
 */
@Slf4j
@Component
public class EnrichmentCache {

    // Step 1에서 설정
    private volatile List<BookEnrichmentTarget> pendingTargets = Collections.emptyList();

    // Step 2에서 수집 (Thread-safe)
    private final ConcurrentLinkedQueue<AladinEnrichmentData> successResults = new ConcurrentLinkedQueue<>();
    private final ConcurrentLinkedQueue<FailedEnrichment> failedResults = new ConcurrentLinkedQueue<>();

    // ========== Step 1: PENDING 도서 목록 ==========

    /**
     * PENDING 도서 목록 설정
     */
    public void setPendingTargets(List<BookEnrichmentTarget> targets) {
        this.pendingTargets = targets != null ? new ArrayList<>(targets) : Collections.emptyList();
    }

    /**
     * PENDING 도서 목록 조회
     */
    public List<BookEnrichmentTarget> getPendingTargets() {
        return pendingTargets;
    }

    /**
     * PENDING 도서 수 조회
     */
    public int getPendingCount() {
        return pendingTargets.size();
    }

    // ========== Step 2: API 호출 결과 수집 ==========

    /**
     * 성공 결과 추가 (Thread-safe)
     */
    public void addSuccess(AladinEnrichmentData data) {
        successResults.add(data);
    }

    /**
     * 실패 결과 추가 (Thread-safe)
     */
    public void addFailure(Long bookId, Long batchId, String reason) {
        failedResults.add(new FailedEnrichment(bookId, batchId, reason));
    }

    /**
     * 성공 결과 수 조회
     */
    public int getSuccessCount() {
        return successResults.size();
    }

    /**
     * 실패 결과 수 조회
     */
    public int getFailedCount() {
        return failedResults.size();
    }

    // ========== Step 3: 결과 조회 ==========

    /**
     * 성공 결과 목록 조회 (List로 변환)
     */
    public List<AladinEnrichmentData> getSuccessResults() {
        return new ArrayList<>(successResults);
    }

    /**
     * 실패 결과 목록 조회 (List로 변환)
     */
    public List<FailedEnrichment> getFailedResults() {
        return new ArrayList<>(failedResults);
    }

    // ========== 캐시 초기화 ==========

    /**
     * 모든 캐시 초기화 (Job 시작/종료 시 호출)
     */
    public void clear() {
        pendingTargets = Collections.emptyList();
        successResults.clear();
        failedResults.clear();
    }

    /**
     * 결과만 초기화 (Step 3 완료 후)
     */
    public void clearResults() {
        successResults.clear();
        failedResults.clear();
    }

    /**
     * 처리 결과 요약 로그
     */
    public void logSummary() {
        log.info("[EnrichmentCache] 처리 결과 - PENDING: {}, 성공: {}, 실패: {}",
                pendingTargets.size(), successResults.size(), failedResults.size());
    }
}
