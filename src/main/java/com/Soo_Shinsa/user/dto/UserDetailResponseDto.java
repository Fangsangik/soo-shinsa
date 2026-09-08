package com.Soo_Shinsa.user.dto;

import com.Soo_Shinsa.user.model.Grade;
import com.Soo_Shinsa.user.model.User;
import lombok.Getter;

@Getter
public class UserDetailResponseDto {
    private final String name;
    private final String phoneNum;
    private final String role;
    private java.math.BigDecimal point;
    private java.math.BigDecimal totalPurchase;
    private String userGrade;
    private String pointRate;
    private String requirement;

    public UserDetailResponseDto(User user) {
        this.name = user.getName();
        this.phoneNum = user.getPhoneNum();
        this.role = user.getRole().name();
        this.point = user.getPoint();
        this.totalPurchase = user.getTotalPurchase();
        if(user.getUserGrade() != null){
            Grade grade = user.getUserGrade().getGrade();
            this.userGrade = grade.getName().getName();
            this.pointRate = grade.getPointRate().toString();
            this.requirement = grade.getRequirement().toString();

        }
    }
}
