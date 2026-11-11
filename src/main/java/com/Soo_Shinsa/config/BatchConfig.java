package com.Soo_Shinsa.config;

import com.Soo_Shinsa.statistics.model.Statistics;
import com.Soo_Shinsa.statistics.repository.StatisticsRepository;
import com.Soo_Shinsa.statistics.dto.OrderHistoryForStatistic;
import jakarta.persistence.EntityManagerFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
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
import org.springframework.transaction.PlatformTransactionManager;

@Slf4j
@RequiredArgsConstructor
@Configuration
// @EnableBatchProcessing 를 두면 Boot 3 의 배치 자동설정(메타테이블 생성 등)이 꺼진다 → 제거
public class BatchConfig {

    private final JobRepository jobRepository;
    private final EntityManagerFactory entityManagerFactory;
    private final PlatformTransactionManager platformTransactionManager;

    private final StatisticsRepository statisticsRepository;


    @Bean
    public Job stackOrderHistoryJob() {
        return new JobBuilder("stackOrderHistoryJob", jobRepository)
                .start(stackOrderHistoryStep())
                .build();
    }

    @Bean
    public Step stackOrderHistoryStep() {

        return new StepBuilder("stackOrderHistoryStep", jobRepository)
                .<OrderHistoryForStatistic, Statistics>chunk(100, platformTransactionManager)
                .reader(orderHistoryReader(null))       // 읽기 메서드 자리
                .processor(processor())
                .writer(statisticWriter())
                .build();
    }


    @Bean
    @org.springframework.batch.core.configuration.annotation.StepScope
    public JpaPagingItemReader<OrderHistoryForStatistic> orderHistoryReader(@org.springframework.beans.factory.annotation.Value("#{jobParameters['date']}") String date) {
        JpaNativeQueryProvider<OrderHistoryForStatistic> queryProvider = new JpaNativeQueryProvider<>();
        queryProvider.setSqlQuery(
                // 컬럼 순서 = OrderHistoryForStatistic 생성자 순서
                "select o.status as orderStatus, o.created_at as orderDate, oi.quantity as quantity, " +
                        "coalesce(oi.discount_price, oi.price, p.price) * oi.quantity as price, " +
                        "p.name as productName, c.name as categoryName, b.name as brandName " +
                        "from orders o " +
                        "join order_items oi on oi.orders_id = o.id " +
                        "join product p on p.id = oi.product_id " +
                        "join brand b on b.id = p.brand_id " +
                        "left join sub_category sc on sc.id = b.sub_category_id " +
                        "left join category c on c.id = sc.category_id " +
                        "where DATE(o.created_at) = :targetDate " +
                        "order by oi.id"
        );
        queryProvider.setEntityClass(OrderHistoryForStatistic.class);

        return new JpaPagingItemReaderBuilder<OrderHistoryForStatistic>()
                .name("nativeQueryReaderWithParams")
                .entityManagerFactory(entityManagerFactory)
                .queryProvider(queryProvider)
                .parameterValues(java.util.Map.of("targetDate", java.sql.Date.valueOf(java.time.LocalDate.parse(date))))
                .pageSize(10)
                .build();
    }

    @Bean
    public ItemProcessor<OrderHistoryForStatistic, Statistics> processor() {
        return Statistics::new;
    }

    @Bean
    public RepositoryItemWriter<Statistics> statisticWriter() {
        return new RepositoryItemWriterBuilder<Statistics>()
                .repository(statisticsRepository)
                .methodName("save")
                .build();
    }

}
