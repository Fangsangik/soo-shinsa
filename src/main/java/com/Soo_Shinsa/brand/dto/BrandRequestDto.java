package com.Soo_Shinsa.brand.dto;

import com.Soo_Shinsa.constant.BrandStatus;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Getter
public class BrandRequestDto {

    @NotEmpty(message = "등록번호는 필수입니다.")
    private String registrationNum;

    @NotEmpty(message = "브랜드명은 필수입니다.")
    private String name;

    @NotEmpty(message = "브랜드 소개는 필수입니다.")
    private String context;

    @NotNull(message = "브랜드 상태는 필수입니다.")
    private BrandStatus status;

    @NotNull(message = "카테고리 번호는 필수입니다.")
    private Long subCategoryId;

}
