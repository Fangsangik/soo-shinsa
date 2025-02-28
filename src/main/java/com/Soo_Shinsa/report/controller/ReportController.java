package com.Soo_Shinsa.report.controller;

import com.Soo_Shinsa.global.auth.UserDetailsImp;
import com.Soo_Shinsa.global.utils.CommonResponse;
import com.Soo_Shinsa.global.utils.ResponseMessage;
import com.Soo_Shinsa.global.utils.UserUtils;
import com.Soo_Shinsa.report.dto.ReportProcessDto;
import com.Soo_Shinsa.report.dto.ReportRequestDto;
import com.Soo_Shinsa.report.dto.ReportResponseDto;
import com.Soo_Shinsa.report.service.ReportService;
import com.Soo_Shinsa.user.model.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @PostMapping
    public ResponseEntity<CommonResponse<ReportResponseDto>> createReport(@Valid @RequestBody ReportRequestDto requestDto,
                                                                         @AuthenticationPrincipal UserDetailsImp userDetails) {
        User user = UserUtils.getUser(userDetails);
        ReportResponseDto report = reportService.createReport(requestDto, user);
        CommonResponse<ReportResponseDto> response = new CommonResponse<>(ResponseMessage.REPORT_CREATE_SUCCESS, report);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PatchMapping("/admin/{reportId}/status")
    public ResponseEntity<Void> reportProcess (@PathVariable Long reportId,
                                               @Valid @RequestBody ReportProcessDto processDto,
                                               @AuthenticationPrincipal UserDetailsImp userDetails) {
        User user = UserUtils.getUser(userDetails);
        reportService.processReport(reportId, processDto, user);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/admin/{reportId}")
    public ResponseEntity<CommonResponse<ReportResponseDto>> getReport(@PathVariable Long reportId,
                                                       @AuthenticationPrincipal UserDetailsImp userDetails) {
        User user = UserUtils.getUser(userDetails);
        ReportResponseDto report = reportService.getReport(reportId, user);
        CommonResponse<ReportResponseDto> response = new CommonResponse<>(ResponseMessage.REPORT_SELECT_SUCCESS, report);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @DeleteMapping("/admin/{reportId}")
    public ResponseEntity<Void> deleteReport(@PathVariable Long reportId,
                                             @AuthenticationPrincipal UserDetailsImp userDetails) {
        User user = UserUtils.getUser(userDetails);
        reportService.deleteReport(reportId, user);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/admin")
    public ResponseEntity<CommonResponse<?>> getAllReports(@AuthenticationPrincipal UserDetailsImp userDetails,
                                           @RequestParam(defaultValue = "0") int page,
                                           @RequestParam(defaultValue = "10") int size) {
        User user = UserUtils.getUser(userDetails);
        Page<ReportResponseDto> reports = reportService.getAllReports(user, page, size);
        CommonResponse< Page<ReportResponseDto>> response = new CommonResponse<>(ResponseMessage.REPORT_SELECT_SUCCESS, reports);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }
}
