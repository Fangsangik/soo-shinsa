package com.Soo_Shinsa.brand.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor  
public class BrandRejectionDto {
    
    @NotBlank(message = "거절 사유는 필수입니다.")
    private String rejectionReason;
    
    private String adminComment;
    
    public BrandRejectionDto(String rejectionReason, String adminComment) {
        this.rejectionReason = rejectionReason;
        this.adminComment = adminComment;
    }
}