package com.Soo_Shinsa.report.dto;

import com.Soo_Shinsa.constant.ReportStatus;
import com.Soo_Shinsa.constant.TargetType;
import com.Soo_Shinsa.report.model.Report;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ReportResponseDto {
    private Long id;
    private Long targetId;
    private TargetType targetType;
    private ReportStatus status;
    private String content;
    private String rejectReason; // 반려 시 신고자에게 보여줄 사유

    @Builder
    public ReportResponseDto(Long id, Long targetId, TargetType targetType, ReportStatus status, String content, String rejectReason) {
        this.id = id;
        this.targetId = targetId;
        this.targetType = targetType;
        this.status = status;
        this.content = content;
        this.rejectReason = rejectReason;
    }

    public static ReportResponseDto toDto(Report report) {
        return ReportResponseDto.builder()
                .id(report.getId())
                .targetId(report.getTargetId())
                .targetType(report.getTargetType())
                .status(report.getStatus())
                .content(report.getContent())
                .rejectReason(report.getRejectReason())
                .build();
    }
}
