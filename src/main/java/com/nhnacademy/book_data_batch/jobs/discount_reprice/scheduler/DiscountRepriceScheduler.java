package com.nhnacademy.book_data_batch.jobs.discount_reprice.scheduler;

import com.nhnacademy.book_data_batch.domain.entity.DiscountPolicy;
import com.nhnacademy.book_data_batch.domain.repository.CategoryRepository;
import com.nhnacademy.book_data_batch.domain.repository.DiscountPolicyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class DiscountRepriceScheduler {

    private final JobLauncher jobLauncher;
    private final Job discountRepriceJob;
    private final DiscountPolicyRepository discountPolicyRepository;
    private final CategoryRepository categoryRepository;

    @Scheduled(cron = "0 0 0 * * *", zone = "Asia/Seoul")
    public void scheduleDiscountReprice() {
        LocalDate today = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);
        
        log.info("할인 재계산 스케줄러 시작 (기준일: {})", today);

        // 1. 대상 정책 식별 (어제 종료되었거나, 오늘 시작되는 정책)
        // 종료일이 어제 -> 오늘부터 할인이 적용 안 되어야 함 (정가 복귀 or 다른 정책 적용)
        List<DiscountPolicy> expiredPolicies = discountPolicyRepository.findByEndDate(yesterday);
        
        // 시작일이 오늘 -> 오늘부터 할인 적용 되어야 함
        List<DiscountPolicy> startingPolicies = discountPolicyRepository.findByStartDate(today);

        Set<Long> targetCategoryIds = new HashSet<>();
        boolean runAll = false;

        // 종료된 정책 처리
        for (DiscountPolicy p : expiredPolicies) {
            if (p.getCategory() == null) {
                runAll = true;
            } else {
                targetCategoryIds.add(p.getCategory().getId());
            }
        }
        
        // 시작된 정책 처리
        for (DiscountPolicy p : startingPolicies) {
             if (p.getCategory() == null) {
                runAll = true;
            } else {
                targetCategoryIds.add(p.getCategory().getId());
            }
        }
        
        if (targetCategoryIds.isEmpty() && !runAll) {
            log.info("변경 대상 할인 정책 없음. 스케줄러 종료.");
            return;
        }

        // 2. 배치 실행
        try {
            if (runAll) {
                log.info("전역 정책 변경 감지 -> 전체 도서 재계산 실행");
                runJob("ALL", null, null);
            } else {
                log.info("카테고리 정책 변경 감지 -> 대상 카테고리 수: {}", targetCategoryIds.size());
                for (Long categoryId : targetCategoryIds) {
                    String categoryPath = categoryRepository.findPathByCategoryId(categoryId);
                    if (categoryPath != null) {
                        runJob("CATEGORY", categoryId, categoryPath);
                    } else {
                        log.warn("카테고리 ID {} 에 해당하는 경로를 찾을 수 없습니다.", categoryId);
                    }
                }
            }
        } catch (Exception e) {
            log.error("스케줄러 실행 중 오류 발생", e);
        }
    }

    private void runJob(String targetScope, Long categoryId, String categoryPath) throws Exception {
        JobParametersBuilder builder = new JobParametersBuilder()
                .addLong("launchTimestamp", System.currentTimeMillis())
                .addString("asOfDate", LocalDate.now().toString())
                .addString("targetScope", targetScope);

        if (categoryId != null) {
            builder.addLong("categoryId", categoryId);
            builder.addString("categoryPath", categoryPath);
        }

        JobParameters parameters = builder.toJobParameters();
        jobLauncher.run(discountRepriceJob, parameters);
        
        // 너무 빠른 반복 실행 방지 (선택 사항, 여기선 생략)
    }
}
