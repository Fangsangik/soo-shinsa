package com.Soo_Shinsa.statistics;

import com.Soo_Shinsa.statistics.dto.OrderHistoryForStatistic;
import com.Soo_Shinsa.statistics.model.Statistics;
import com.Soo_Shinsa.statistics.repository.StatisticsRepository;
import jakarta.persistence.EntityManagerFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.CronScheduleBuilder;
import org.quartz.JobDetail;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.springframework.batch.core.*;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.data.RepositoryItemWriter;
import org.springframework.batch.item.data.builder.RepositoryItemWriterBuilder;
import org.springframework.batch.item.database.JpaPagingItemReader;
import org.springframework.batch.item.database.builder.JpaPagingItemReaderBuilder;
import org.springframework.batch.item.database.orm.JpaNativeQueryProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.PlatformTransactionManager;

import static org.quartz.JobBuilder.newJob;

/**
 * EnableBatchProcessing : Spring Batch 기능 활성화
 * EnableScheduling : Quatz 스케줄링 기능 활성화
 */
@Slf4j
@RequiredArgsConstructor
@Configuration
@EnableBatchProcessing
@EnableScheduling
public class BatchConfig {

    private final JobRepository jobRepository;
    private final EntityManagerFactory entityManagerFactory;
    private final PlatformTransactionManager platformTransactionManager;
    private final StatisticsRepository statisticsRepository;

    /**
     * stackOrderHistoryJob은 stackOrderHistoryStep을 실행하는 Job
     * jobRepository를 사용하여 Job 실행 이력 저장
     * @return
     */
    @Bean
    public Job stackOrderHistoryJob() {
        return new JobBuilder("stackOrderHistoryJob", jobRepository)
                .start(stackOrderHistoryStep())
                .build();
    }

    /**
     * 읽기 -> 가공 -> 쓰기를 수행하는 Step
     * 한번에 100개 데이터 처리
     * @return
     */
    @Bean
    public Step stackOrderHistoryStep() {
        return new StepBuilder("stackOrderHistoryStep", jobRepository)
                .<OrderHistoryForStatistic, Statistics>chunk(100, platformTransactionManager)
                .reader(orderHistoryReader())
                .processor(processor())
                .writer(statisticWriter())
                .listener(new StepExecutionListener() {
                    @Override
                    public void beforeStep(StepExecution stepExecution) {
                        log.info("➡ stackOrderHistoryStep() STARTED");
                    }

                    @Override
                    public ExitStatus afterStep(StepExecution stepExecution) {
                        log.info("✅ stackOrderHistoryStep() COMPLETED - {} items processed", stepExecution.getWriteCount());
                        return ExitStatus.COMPLETED;
                    }
                })
                .build();
    }

    /**
     * JPA를 이용해 어제 날짜 주문 데이터 조회
     * @return
     */
    @Bean
    public JpaPagingItemReader<OrderHistoryForStatistic> orderHistoryReader() {
        JpaNativeQueryProvider<OrderHistoryForStatistic> queryProvider = new JpaNativeQueryProvider<>();
        queryProvider.setSqlQuery("SELECT o.status as orderStatus, o.order_date as orderDate, " +
                "oi.quantity as quantity, p.price as price, p.name as productName, b.name as brandName " +
                "FROM orders o " +
                "JOIN order_items oi ON o.id = oi.orders_id " +
                "JOIN product p ON oi.product_id = p.id " +
                "JOIN brand b ON p.brand_id = b.id " +
                "WHERE o.order_date BETWEEN DATE_SUB(NOW(), INTERVAL 1 DAY) AND NOW()");
        queryProvider.setEntityClass(OrderHistoryForStatistic.class);

        return new JpaPagingItemReaderBuilder<OrderHistoryForStatistic>()
                .name("orderHistoryReader")
                .entityManagerFactory(entityManagerFactory)
                .queryProvider(queryProvider)
                .pageSize(50)
                .build();
    }

    /**
     * 조회한 데이터를 가공하여 새로운 Statistics 객체 생성
     * @return
     */
    @Bean
    public ItemProcessor<OrderHistoryForStatistic, Statistics> processor() {
        return Statistics::new;
    }

    /**
     * 가공한 데이터를 DB에 저장
     * @return
     */
    @Bean
    public RepositoryItemWriter<Statistics> statisticWriter() {
        return new RepositoryItemWriterBuilder<Statistics>()
                .repository(statisticsRepository)
                .methodName("save")
                .build();
    }

    /**
     * Quartz를 이용한 스케줄링
     * BatchJob을 Quartz Job으로 등록
     * @return
     */
    @Bean
    public JobDetail batchJobDetail() {
        return newJob(BatchJob.class)
                .withIdentity("batchJob")
                .storeDurably()
                .build();
    }

    /**
     * 매일 00시에 BatchJob 실행
     * @return
     */
    @Bean
    public Trigger batchJobTrigger() {
        return TriggerBuilder.newTrigger()
                .forJob(batchJobDetail())
                .withIdentity("batchJobTrigger")
                .withSchedule(CronScheduleBuilder.cronSchedule("0 0 0 * * ?"))
                .build();
    }
}
