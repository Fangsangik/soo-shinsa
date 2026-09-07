package com.Soo_Shinsa.brand.dto;

import com.Soo_Shinsa.brand.model.Brand;
import com.Soo_Shinsa.global.constant.BrandStatus;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class FindBrandAllResponseDto {
    private Long id;
    private String name;
    private String context;
    private BrandStatus status;

    @Builder
    public FindBrandAllResponseDto(Long id, String name, String context, BrandStatus status) {
        this.id = id;
        this.name = name;
        this.context = context;
        this.status = status;
    }

    public static FindBrandAllResponseDto of(Brand brand) {
        return FindBrandAllResponseDto.builder()
                .id(brand.getId())
                .name(brand.getName())
                .context(brand.getContext())
                .status(brand.getStatus())
                .build();
    }
}
