package com.Soo_Shinsa.user.model;

import com.Soo_Shinsa.global.constant.BaseTimeEntity;
import com.Soo_Shinsa.global.constant.GradeType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Getter
@NoArgsConstructor
public class Grade extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long gradeId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private GradeType name;

    @Column(nullable = false)
    private BigDecimal pointRate;

    @Column(nullable = false)
    private BigDecimal requirement;


    public Grade(GradeType name, BigDecimal pointRate, BigDecimal requirement) {
        this.name = name;
        this.pointRate = pointRate;
        this.requirement = requirement;
    }
}
