package com.Soo_Shinsa.report.dto;

import com.Soo_Shinsa.global.constant.ReportStatus;
import com.Soo_Shinsa.global.constant.TargetType;
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

    @Builder
    public ReportResponseDto(Long id, Long targetId, TargetType targetType, ReportStatus status, String content) {
        this.id = id;
        this.targetId = targetId;
        this.targetType = targetType;
        this.status = status;
        this.content = content;
    }

    public static ReportResponseDto toDto(Report report) {
        return ReportResponseDto.builder()
                .id(report.getId())
                .targetId(report.getTargetId())
                .targetType(report.getTargetType())
                .status(report.getStatus())
                .content(report.getContent())
                .build();
    }
}
