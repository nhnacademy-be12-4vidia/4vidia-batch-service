package com.nhnacademy.book_data_batch.global.batch;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.boot.ApplicationArguments;
import org.springframework.context.ApplicationContext;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
@DisplayName("ManualJobLauncher 테스트")
class ManualJobLauncherTest {

    @Mock
    private JobLauncher jobLauncher;

    @Mock
    private ApplicationContext applicationContext;

    @Mock
    private ApplicationArguments applicationArguments;

    @Mock
    private Job job;

    private ManualJobLauncher manualJobLauncher;

    @BeforeEach
    void setUp() {
        manualJobLauncher = new ManualJobLauncher(jobLauncher, applicationContext);
    }

    @Test
    @DisplayName("spring.batch.job.name이 비어있으면 Job 실행하지 않음")
    void testRunWithEmptyJobName() throws Exception {
        // Given: spring.batch.job.name이 빈 문자열
        ReflectionTestUtils.setField(manualJobLauncher, "requestedJobNames", "");

        // When: run 메서드 호출
        manualJobLauncher.run(applicationArguments);

        // Then: JobLauncher.run이 호출되지 않음
        verify(jobLauncher, never()).run(any(Job.class), any(JobParameters.class));
    }

    @Test
    @DisplayName("spring.batch.job.name이 null이면 Job 실행하지 않음")
    void testRunWithNullJobName() throws Exception {
        // Given: spring.batch.job.name이 null
        ReflectionTestUtils.setField(manualJobLauncher, "requestedJobNames", null);

        // When: run 메서드 호출
        assertDoesNotThrow(() -> manualJobLauncher.run(applicationArguments));

        // Then: JobLauncher.run이 호출되지 않음
        verify(jobLauncher, never()).run(any(Job.class), any(JobParameters.class));
    }

    @Test
    @DisplayName("단일 Job 이름이 지정되면 해당 Job 실행")
    void testRunWithSingleJobName() throws Exception {
        // Given: spring.batch.job.name이 "testJob"
        ReflectionTestUtils.setField(manualJobLauncher, "requestedJobNames", "testJob");
        when(applicationContext.getBean("testJob", Job.class)).thenReturn(job);

        // When: run 메서드 호출
        manualJobLauncher.run(applicationArguments);

        // Then: ApplicationContext에서 Job을 조회하고 실행
        verify(applicationContext).getBean("testJob", Job.class);
        ArgumentCaptor<Job> jobCaptor = ArgumentCaptor.forClass(Job.class);
        ArgumentCaptor<JobParameters> parametersCaptor = ArgumentCaptor.forClass(JobParameters.class);
        verify(jobLauncher).run(jobCaptor.capture(), parametersCaptor.capture());

        assertEquals(job, jobCaptor.getValue());
        assertNotNull(parametersCaptor.getValue());
    }

    @Test
    @DisplayName("여러 Job 이름이 쉼표로 구분되면 순차적으로 실행")
    void testRunWithMultipleJobNames() throws Exception {
        // Given: spring.batch.job.name이 "job1,job2"
        ReflectionTestUtils.setField(manualJobLauncher, "requestedJobNames", "job1, job2");

        Job job1 = mock(Job.class);
        Job job2 = mock(Job.class);

        when(applicationContext.getBean("job1", Job.class)).thenReturn(job1);
        when(applicationContext.getBean("job2", Job.class)).thenReturn(job2);

        // When: run 메서드 호출
        manualJobLauncher.run(applicationArguments);

        // Then: 두 Job이 순차적으로 실행됨
        ArgumentCaptor<Job> jobCaptor = ArgumentCaptor.forClass(Job.class);
        verify(jobLauncher, times(2)).run(jobCaptor.capture(), any(JobParameters.class));

        // Job 실행 순서 확인
        assertEquals(job1, jobCaptor.getAllValues().get(0));
        assertEquals(job2, jobCaptor.getAllValues().get(1));
    }

    @Test
    @DisplayName("Job 이름에 공백이 있으면 트림 후 실행")
    void testRunWithWhitespaceInJobName() throws Exception {
        // Given: spring.batch.job.name에 공백이 포함됨
        ReflectionTestUtils.setField(manualJobLauncher, "requestedJobNames", "  testJob  , anotherJob  ");

        when(applicationContext.getBean("testJob", Job.class)).thenReturn(job);
        when(applicationContext.getBean("anotherJob", Job.class)).thenReturn(mock(Job.class));

        // When: run 메서드 호출
        manualJobLauncher.run(applicationArguments);

        // Then: 공백이 제거된 이름으로 Job을 조회
        verify(applicationContext).getBean("testJob", Job.class);
        verify(applicationContext).getBean("anotherJob", Job.class);
    }

    @Test
    @DisplayName("JobParameters에 launchTimestamp가 포함됨")
    void testJobParametersIncludesLaunchTimestamp() throws Exception {
        // Given: spring.batch.job.name이 "testJob"
        ReflectionTestUtils.setField(manualJobLauncher, "requestedJobNames", "testJob");
        when(applicationContext.getBean("testJob", Job.class)).thenReturn(job);

        // When: run 메서드 호출
        long beforeExecution = System.currentTimeMillis();
        manualJobLauncher.run(applicationArguments);
        long afterExecution = System.currentTimeMillis();

        // Then: JobParameters에 launchTimestamp 포함
        ArgumentCaptor<JobParameters> parametersCaptor = ArgumentCaptor.forClass(JobParameters.class);
        verify(jobLauncher).run(any(Job.class), parametersCaptor.capture());

        JobParameters params = parametersCaptor.getValue();
        assertNotNull(params.getLong("launchTimestamp"),
            "launchTimestamp 파라미터가 있어야 함");

        Long timestamp = params.getLong("launchTimestamp");
        assertTrue(timestamp >= beforeExecution && timestamp <= afterExecution,
            "launchTimestamp가 실행 시간 범위 내에 있어야 함");
    }

    @Test
    @DisplayName("쉼표만 있거나 공백만 있는 경우 Job 실행하지 않음")
    void testRunWithOnlyWhitespaceAndCommas() throws Exception {
        // Given: spring.batch.job.name이 " , , "
        ReflectionTestUtils.setField(manualJobLauncher, "requestedJobNames", " , , ");

        // When: run 메서드 호출
        manualJobLauncher.run(applicationArguments);

        // Then: Job이 실행되지 않음
        verify(jobLauncher, never()).run(any(Job.class), any(JobParameters.class));
    }

    @Test
    @DisplayName("Job을 찾을 수 없으면 예외 발생")
    void testRunWithNonExistentJob() throws Exception {
        // Given: spring.batch.job.name이 "nonExistentJob"
        ReflectionTestUtils.setField(manualJobLauncher, "requestedJobNames", "nonExistentJob");
        when(applicationContext.getBean("nonExistentJob", Job.class))
            .thenThrow(new org.springframework.beans.factory.NoSuchBeanDefinitionException("Job not found"));

        // When & Then: 예외 발생
        assertThrows(org.springframework.beans.factory.NoSuchBeanDefinitionException.class,
            () -> manualJobLauncher.run(applicationArguments));
    }
}
