package com.Soo_Shinsa.statistics.service;

import com.Soo_Shinsa.statistics.repository.StatisticsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

/** 하루치 주문을 statistics 테이블로 집계. 같은 날짜를 다시 돌리면 기존 집계를 지우고 재생성(멱등). 야간 스케줄과 관리자 수동 실행이 공유한다. */
@Slf4j
@Service
@RequiredArgsConstructor
public class StatisticsAggregateService {
    private final JobLauncher jobLauncher;
    private final Job stackOrderHistoryJob;
    private final StatisticsRepository statisticsRepository;

    public long aggregate(LocalDate date) {
        statisticsRepository.deleteByOrderDate(date);
        try {
            JobExecution exec = jobLauncher.run(stackOrderHistoryJob, new JobParametersBuilder()
                    .addString("date", date.toString())
                    .addLong("run", System.currentTimeMillis()) // 같은 날짜 재실행 허용
                    .toJobParameters());
            if (!exec.getStatus().isUnsuccessful()) {
                return statisticsRepository.countByOrderDate(date);
            }
            throw new IllegalStateException("집계 배치 실패: " + exec.getAllFailureExceptions());
        } catch (org.springframework.batch.core.JobExecutionException e) {
            throw new IllegalStateException("집계 배치를 시작할 수 없습니다: " + e.getMessage(), e);
        }
    }
}
