package com.nhnacademy.book_data_batch.batch.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class ContentImageCleanupScheduler {

    private final JobLauncher jobLauncher;
    private final Job contentImageCleanupJob;

    // 매일 새벽 3시에 실행 (초 분 시 일 월 요일)
    @Scheduled(cron = "0 0 3 * * *")
    public void runCleanupJob() {
        log.info("Starting ContentImageCleanupJob at {}", LocalDateTime.now());
        try {
            JobParameters jobParameters = new JobParametersBuilder()
                    .addString("datetime", LocalDateTime.now().toString()) // 유니크 파라미터
                    .toJobParameters();

            jobLauncher.run(contentImageCleanupJob, jobParameters);
            
            log.info("ContentImageCleanupJob finished successfully.");
        } catch (Exception e) {
            log.error("Failed to run ContentImageCleanupJob", e);
        }
    }
}
