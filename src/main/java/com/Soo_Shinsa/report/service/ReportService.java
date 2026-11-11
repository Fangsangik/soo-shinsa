package com.Soo_Shinsa.report.service;

import com.Soo_Shinsa.report.dto.ReportProcessDto;
import com.Soo_Shinsa.report.dto.ReportRequestDto;
import com.Soo_Shinsa.report.dto.ReportResponseDto;
import com.Soo_Shinsa.user.model.User;
import org.springframework.data.domain.Page;


public interface ReportService {
    ReportResponseDto createReport(ReportRequestDto requestDto, User user);
    void processReport(Long reportId, ReportProcessDto processDto, User user);
    ReportResponseDto getReport(Long reportId, User user);
    void deleteReport(Long reportId, User user);
    Page<ReportResponseDto> getAllReports(User user, int page, int size);

    /** 신고자 본인의 신고 내역 (처리 상태·반려 사유 확인용) */
    Page<ReportResponseDto> getMyReports(User user, int page, int size);
}
