package com.Soo_Shinsa.statistics;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.JobExecutionContext;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class BatchJob implements org.quartz.Job {
    private final JobLauncher jobLauncher;
    private final org.springframework.batch.core.Job stackOrderHistoryJob;

    /**
     * Quartz가 execute 메소드를 실행
     * Spring Batch Job을 실행
     * JobParametersBuilder를 사용하여 중복 실행 방지
     * @param context
     */
    @Override
    public void execute(JobExecutionContext context) {
        try {
            log.info("🚀 Executing Quartz Job for Spring Batch 🚀"); // 로그 추가
            jobLauncher.run(stackOrderHistoryJob, new JobParametersBuilder()
                    .addLong("time", System.currentTimeMillis())
                    .toJobParameters());
            log.info("✅ Batch Job Execution Completed ✅");
        } catch (Exception e) {
            log.error("❌ Batch job failed", e);
        }
    }
}