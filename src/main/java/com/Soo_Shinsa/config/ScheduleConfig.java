package com.Soo_Shinsa.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.configuration.JobRegistry;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;
import java.util.Date;

@RequiredArgsConstructor
@Slf4j
@Configuration
public class ScheduleConfig {
    private final com.Soo_Shinsa.statistics.service.StatisticsAggregateService aggregateService;

    //매 00:00 주기로 동작
    @Scheduled(cron = "0 0 0 * * *", zone = "Asia/Seoul")
//    @Scheduled(cron = "1/5 * * * * *", zone = "Asia/Seoul")
    public void runFirstJob() throws Exception {

        log.info("nightly statistics aggregation start");
        aggregateService.aggregate(java.time.LocalDate.now(java.time.ZoneId.of("Asia/Seoul")).minusDays(1));
    }
}