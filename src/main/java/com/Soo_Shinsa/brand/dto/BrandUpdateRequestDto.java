package com.Soo_Shinsa.brand.dto;

import com.Soo_Shinsa.global.constant.BrandStatus;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Getter
public class BrandUpdateRequestDto {

    private String registrationNum;

    private String name;

    private String context;

    private BrandStatus status;

    public BrandUpdateRequestDto(String registrationNum, String name, String context, BrandStatus status) {
        this.registrationNum = registrationNum;
        this.name = name;
        this.context = context;
        this.status = status;
    }
}
