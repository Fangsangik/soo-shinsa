package com.Soo_Shinsa.report.service;

import com.Soo_Shinsa.brand.repository.BrandRepository;
import com.Soo_Shinsa.global.constant.ReportStatus;
import com.Soo_Shinsa.global.constant.TargetType;
import com.Soo_Shinsa.global.exception.ErrorCode;
import com.Soo_Shinsa.global.exception.NotFoundException;
import com.Soo_Shinsa.global.utils.EntityValidator;
import com.Soo_Shinsa.product.repository.ProductRepository;
import com.Soo_Shinsa.report.dto.ReportProcessDto;
import com.Soo_Shinsa.report.dto.ReportRequestDto;
import com.Soo_Shinsa.report.dto.ReportResponseDto;
import com.Soo_Shinsa.report.model.Report;
import com.Soo_Shinsa.report.repository.ReportRepository;
import com.Soo_Shinsa.review.repository.ReviewRepository;
import com.Soo_Shinsa.user.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {

    private final ReportRepository reportRepository;
    private final ReviewRepository reviewRepository;
    private final BrandRepository brandRepository;
    private final ProductRepository productRepository;

    /**
     * 신고 생성
     *
     * @param requestDto
     * @return ReportResponseDto.toDto(report)
     */
    @Transactional
    @Override
    public ReportResponseDto createReport(ReportRequestDto requestDto, User user) {

        if (TargetType.BRAND.equals(requestDto.getTargetType())) {
            if (!brandRepository.existsById(requestDto.getTargetId())) {
                throw new NotFoundException(ErrorCode.NOT_FOUND_BRAND);
            }
        }

        if (TargetType.PRODUCT.equals(requestDto.getTargetType())) {
            if (!productRepository.existsById(requestDto.getTargetId())) {
                throw new NotFoundException(ErrorCode.NOT_FOUND_PRODUCT);
            }
        }

        if (TargetType.REVIEW.equals(requestDto.getTargetType())) {
            if (!reviewRepository.existsById(requestDto.getTargetId())) {
                throw new NotFoundException(ErrorCode.NOT_FOUND_REVIEW);
            }
        }

        Report report = Report.builder()
                .targetId(requestDto.getTargetId())
                .targetType(requestDto.getTargetType())
                .status(requestDto.getStatus())
                .content(requestDto.getContent())
                .user(user)
                .build();

        Report saveReport = reportRepository.save(report);
        return ReportResponseDto.toDto(saveReport);
    }

    /**
     * 신고 처리
     *
     * @param reportId
     * @param processDto
     */
    @Transactional
    @Override
    public void processReport(Long reportId, ReportProcessDto processDto, User user) {
        // 신고 엔티티 조회
        Report report = reportRepository.findByIdOrElseThrow(reportId);

        // 사용자 권한 검증
        EntityValidator.validateAdminAccess(user);

        // 신고 상태 처리
        report.process(processDto.getStatus());

        // 반려 상태라면 반려 사유 추가
        if (ReportStatus.REJECTED.equals(processDto.getStatus())) {
            report.addRejectReason(processDto.getRejectReason());
        } else {
            report.addRejectReason(null); // 반려 사유 초기화
        }
    }

    /**
     * 신고 조회
     *
     * @param reportId
     * @return ReportResponseDto.toDto(report)
     */
    @Override
    public ReportResponseDto getReport(Long reportId, User user) {
        Report report = reportRepository.findByIdOrElseThrow(reportId);

        // 사용자 권한 검증
        EntityValidator.validateAdminAccess(user);
        return ReportResponseDto.toDto(report);
    }

    @Override
    public Page<ReportResponseDto> getAllReports(User user, int page, int size) {
        EntityValidator.validateAdminAccess(user);
        Pageable pageable = PageRequest.of(page, size);
        Page<Report> reports = reportRepository.findAllReports(pageable);
        return reports.map(ReportResponseDto::toDto);
    }

    /**
     * 신고 삭제
     *
     * @param reportId
     */
    @Transactional
    @Override
    public void deleteReport(Long reportId, User user) {
        if (!reportRepository.existsById(reportId)) {
            throw new NotFoundException(ErrorCode.NOT_FOUND_REPORT);
        }

        EntityValidator.validateAdminAccess(user);

        reportRepository.deleteById(reportId);
    }
}
