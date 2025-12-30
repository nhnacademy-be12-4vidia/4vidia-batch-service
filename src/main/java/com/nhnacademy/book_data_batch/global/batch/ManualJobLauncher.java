package com.nhnacademy.book_data_batch.global.batch;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Slf4j
@Component
@RequiredArgsConstructor
public class ManualJobLauncher implements ApplicationRunner {

    private final JobLauncher jobLauncher;
    private final ApplicationContext applicationContext;

    @Value("${spring.batch.job.name:}")
    private String requestedJobNames;

    @Override
    public void run(ApplicationArguments args) throws Exception {

        // Job 이름이 비어 있으면 아무 것도 실행 안함
        if (!StringUtils.hasText(requestedJobNames)) {
            log.info("spring.batch.job.name 값이 없어 수동 배치를 실행하지 않습니다.");
            return;
        }

        // 쉼표로 구분된 각 Job 이름에 대해 배치 잡 실행
        for (String name : requestedJobNames.split(",")) {
            String jobName = name.trim();
            if (!StringUtils.hasText(jobName)) {
                continue;
            }
            Job job = applicationContext.getBean(jobName, Job.class);
            JobParameters parameters = new JobParametersBuilder()
                .addLong("launchTimestamp", System.currentTimeMillis())
                .toJobParameters();
            log.info("[BATCH START] Job name: {} 을(를) 매개변수 {} 로 실행합니다.", jobName, parameters);
            jobLauncher.run(job, parameters);
        }
    }
}
