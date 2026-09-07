package com.Soo_Shinsa.order.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
public class PartialCancelRequestDto {
    
    @NotEmpty(message = "취소할 주문 아이템을 선택해주세요")
    private List<Long> orderItemIds;
    
    @NotNull(message = "취소 사유를 입력해주세요")
    @Size(min = 1, max = 500, message = "취소 사유는 1자 이상 500자 이하로 입력해주세요")
    private String cancelReason;

    public PartialCancelRequestDto(List<Long> orderItemIds, String cancelReason) {
        this.orderItemIds = orderItemIds;
        this.cancelReason = cancelReason;
    }
}