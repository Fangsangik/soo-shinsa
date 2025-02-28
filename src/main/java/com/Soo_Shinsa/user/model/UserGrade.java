package com.Soo_Shinsa.user.model;

import com.Soo_Shinsa.global.constant.BaseCreatedTimeEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Entity
public class UserGrade extends BaseCreatedTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long userGradeId;

    @ManyToOne
    @JoinColumn(name = "grade_id")
    private Grade grade;

    public UserGrade(Grade grade) {
        this.grade = grade;
    }
}
