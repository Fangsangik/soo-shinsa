package com.Soo_Shinsa.report.dto;

import com.Soo_Shinsa.global.constant.ReportStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ReportProcessDto {

    @NotNull(message = "신고 값은 필수입니다.")
    private ReportStatus status;

    private String rejectReason;

    public ReportProcessDto(ReportStatus status, String rejectReason) {
        this.status = status;
        this.rejectReason = rejectReason;
    }
}
