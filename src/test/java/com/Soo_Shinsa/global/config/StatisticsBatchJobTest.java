package com.Soo_Shinsa.global.config;

import com.Soo_Shinsa.support.IntegrationTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 매일 00:00 에 도는 통계 배치가 실제로 실행되는지 확인한다.
 *
 * BATCH_JOB_INSTANCE 등 메타데이터 테이블이 없어서 이 잡은 계속 실패하고 있었다.
 * (@EnableBatchProcessing 을 쓰면 Boot 의 배치 자동설정이 물러나므로
 *  spring.batch.jdbc.initialize-schema 가 테이블을 만들어 주지 않는다.)
 * 이제 V2 마이그레이션이 만든다.
 */
@SpringBootTest
class StatisticsBatchJobTest extends IntegrationTestSupport {

    @Autowired private JobLauncher jobLauncher;
    @Autowired private Job stackOrderHistoryJob;

    @Test
    void 통계_배치가_끝까지_돈다() throws Exception {
        JobExecution execution = jobLauncher.run(
                stackOrderHistoryJob,
                new JobParametersBuilder().addString("date", "2026-01-01").toJobParameters());

        assertEquals(BatchStatus.COMPLETED, execution.getStatus(),
                "배치 메타데이터 테이블이 없으면 여기서 깨진다: " + execution.getAllFailureExceptions());
    }
}
