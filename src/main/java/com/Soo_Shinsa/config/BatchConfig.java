package com.Soo_Shinsa.config;

import com.Soo_Shinsa.statistics.dto.OrderHistoryForStatistic;
import com.Soo_Shinsa.statistics.model.Statistics;
import com.Soo_Shinsa.statistics.repository.StatisticsRepository;
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
@EnableBatchProcessing
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
                .reader(orderHistoryReader())       // 읽기 메서드 자리
                .processor(processor())
                .writer(statisticWriter())
                .build();
    }


    @Bean
    public JpaPagingItemReader<OrderHistoryForStatistic> orderHistoryReader() {
        JpaNativeQueryProvider<OrderHistoryForStatistic> queryProvider = new JpaNativeQueryProvider<>();
        queryProvider.setSqlQuery(
                "select orders.status as orderStatus, orders.order_date as orderDate, order_items.quantity as quantity , product.price as price,product.name as productName, brand.name as brandName " +
                        "from (select id, status, created_at as order_date from orders) orders " +
                        "inner join " +
                        "(select orders_id, quantity, product_id " +
                        " from order_items) order_items " +
                        "inner join " +
                        "(select id, brand_id, price,name " +
                        " from product) product " +
                        "inner join " +
                        "(select id, name " +
                        " from brand) brand " +
                        "on orders.id = order_items.orders_id " +
                        "and " +
                        "order_items.product_id = product.id " +
                        "and " +
                        "product.brand_id = brand.id " +
                        "where DATEDIFF(order_date,now()-1) = 0"
        );
        queryProvider.setEntityClass(OrderHistoryForStatistic.class);

        return new JpaPagingItemReaderBuilder<OrderHistoryForStatistic>()
                .name("nativeQueryReaderWithParams")
                .entityManagerFactory(entityManagerFactory)
                .queryProvider(queryProvider)
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
