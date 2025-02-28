package com.Soo_Shinsa.report.dto;

import com.Soo_Shinsa.global.constant.ReportStatus;
import com.Soo_Shinsa.global.constant.TargetType;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ReportRequestDto {

    @NotNull (message = "신고 대상은 필수값 입니다.")
    private Long targetId;

    @NotNull (message = "신고 대상 타입은 필수값 입니다.")
    private TargetType targetType;

    @NotNull (message = "신고 상태는 필수값 입니다.")
    private ReportStatus status;

    private String content;

    public ReportRequestDto(Long targetId, TargetType targetType, ReportStatus status, String content) {
        this.targetId = targetId;
        this.targetType = targetType;
        this.status = status;
        this.content = content;
    }
}
