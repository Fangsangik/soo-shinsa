package com.Soo_Shinsa.brand.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class BrandApprovalDto {
    
    @NotBlank(message = "승인 사유는 필수입니다.")
    private String approvalReason;
    
    private String adminComment;
    
    public BrandApprovalDto(String approvalReason, String adminComment) {
        this.approvalReason = approvalReason;
        this.adminComment = adminComment;
    }
}