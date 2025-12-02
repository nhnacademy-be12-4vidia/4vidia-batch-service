package com.nhnacademy.book_data_batch.batch.enrichment.listener;

import com.nhnacademy.book_data_batch.batch.enrichment.exception.DailyQuotaExceededException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.stereotype.Component;

/**
 * Aladin Enrichment Step 실행 리스너
 * - 일일 쿼터 초과로 인한 Step 실패를 QUOTA_EXHAUSTED로 처리
 * - Job은 계속 진행 가능하도록 함
 */
@Slf4j
@Component
public class QuotaExhaustedStepListener implements StepExecutionListener {

    /**
     * 쿼터 소진으로 인한 종료 상태
     * - FAILED는 아니지만, COMPLETED도 아닌 특별한 상태
     */
    public static final String QUOTA_EXHAUSTED = "QUOTA_EXHAUSTED";

    @Override
    public void beforeStep(StepExecution stepExecution) {
        // 시작 로그
        String stepName = stepExecution.getStepName();
        log.info("Step 시작: {}", stepName);
    }

    @Override
    public ExitStatus afterStep(StepExecution stepExecution) {
        String stepName = stepExecution.getStepName();
        ExitStatus exitStatus = stepExecution.getExitStatus();

        // 실패한 경우, 원인이 쿼터 초과인지 확인
        if (ExitStatus.FAILED.getExitCode().equals(exitStatus.getExitCode())) {
            Throwable failureException = stepExecution.getFailureExceptions()
                    .stream()
                    .findFirst()
                    .orElse(null);

            if (isQuotaExceededException(failureException)) {
                log.info("Step {} 쿼터 소진으로 종료 (정상 처리). 처리 건수: {}", 
                        stepName, stepExecution.getWriteCount());
                
                // QUOTA_EXHAUSTED 상태로 변경 (FAILED가 아님)
                // Job 레벨에서 이 상태를 COMPLETED로 취급하도록 설정 가능
                return new ExitStatus(QUOTA_EXHAUSTED, "일일 API 쿼터 소진");
            }
        }

        log.info("Step {} 종료: status={}, readCount={}, writeCount={}, skipCount={}", 
                stepName, exitStatus.getExitCode(),
                stepExecution.getReadCount(),
                stepExecution.getWriteCount(),
                stepExecution.getSkipCount());

        return exitStatus;
    }

    /**
     * 예외 체인에서 DailyQuotaExceededException 찾기
     */
    private boolean isQuotaExceededException(Throwable throwable) {
        while (throwable != null) {
            if (throwable instanceof DailyQuotaExceededException) {
                return true;
            }
            throwable = throwable.getCause();
        }
        return false;
    }
}
