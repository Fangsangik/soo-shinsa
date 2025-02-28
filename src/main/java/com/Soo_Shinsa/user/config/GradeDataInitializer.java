package com.Soo_Shinsa.user.config;

import com.Soo_Shinsa.global.constant.GradeType;
import com.Soo_Shinsa.user.model.Grade;
import com.Soo_Shinsa.user.repository.GradeRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;
import java.util.Arrays;

@Configuration
public class GradeDataInitializer {

    @Bean
    CommandLineRunner initGradeData(GradeRepository gradeRepository) {
        return args -> {
            if (gradeRepository.count() == 0) {
                Grade rookie = new Grade(GradeType.ROOKIE, BigDecimal.valueOf(0.01), BigDecimal.valueOf(1000));
                Grade bronze = new Grade(GradeType.BRONZE, BigDecimal.valueOf(0.02), BigDecimal.valueOf(2000));
                Grade silver = new Grade(GradeType.SILVER, BigDecimal.valueOf(0.03), BigDecimal.valueOf(3000));
                Grade gold = new Grade(GradeType.GOLD, BigDecimal.valueOf(0.05), BigDecimal.valueOf(5000));

                gradeRepository.saveAll(Arrays.asList(rookie, bronze, silver, gold));
            }
        };
    }
}
