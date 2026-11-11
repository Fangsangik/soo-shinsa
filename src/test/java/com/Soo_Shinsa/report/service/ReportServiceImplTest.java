package com.Soo_Shinsa.report.service;

import com.Soo_Shinsa.brand.repository.BrandRepository;
import com.Soo_Shinsa.constant.ReportStatus;
import com.Soo_Shinsa.constant.Role;
import com.Soo_Shinsa.constant.TargetType;
import com.Soo_Shinsa.exception.NoAuthorizedException;
import com.Soo_Shinsa.exception.NotFoundException;
import com.Soo_Shinsa.product.repository.ProductRepository;
import com.Soo_Shinsa.report.dto.ReportProcessDto;
import com.Soo_Shinsa.report.dto.ReportRequestDto;
import com.Soo_Shinsa.report.dto.ReportResponseDto;
import com.Soo_Shinsa.report.model.Report;
import com.Soo_Shinsa.report.repository.ReportRepository;
import com.Soo_Shinsa.review.repository.ReviewRepository;
import com.Soo_Shinsa.user.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;

import static com.Soo_Shinsa.support.Fixtures.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReportServiceImplTest {
    @Mock ReportRepository reportRepository;
    @Mock ReviewRepository reviewRepository;
    @Mock BrandRepository brandRepository;
    @Mock ProductRepository productRepository;
    @InjectMocks ReportServiceImpl service;

    User customer, admin;
    Report report;

    @BeforeEach
    void setUp() {
        customer = user(1L, Role.CUSTOMER); admin = user(2L, Role.ADMIN);
        report = withId(Report.builder().targetId(5L).targetType(TargetType.PRODUCT).status(ReportStatus.OPEN).content("가품 의심").user(customer).build(), "id", 9L);
        lenient().when(reportRepository.findByIdOrElseThrow(9L)).thenReturn(report);
    }

    @Test
    void 존재하지_않는_대상은_신고할_수_없다() {
        when(productRepository.existsById(999L)).thenReturn(false);
        assertThatThrownBy(() -> service.createReport(new ReportRequestDto(999L, TargetType.PRODUCT, ReportStatus.OPEN, "x"), customer)).isInstanceOf(NotFoundException.class);
        verify(reportRepository, never()).save(any());
    }

    @Test
    void 처리는_관리자만_반려에는_사유가_필요하다() {
        assertThatThrownBy(() -> service.processReport(9L, new ReportProcessDto(ReportStatus.RESOLVED, null), customer)).isInstanceOf(NoAuthorizedException.class);
        assertThatThrownBy(() -> service.processReport(9L, new ReportProcessDto(ReportStatus.REJECTED, null), admin)).isInstanceOf(IllegalArgumentException.class);

        service.processReport(9L, new ReportProcessDto(ReportStatus.REJECTED, "증거 부족"), admin);
        assertThat(report.getStatus()).isEqualTo(ReportStatus.REJECTED);
        assertThat(report.getRejectReason()).isEqualTo("증거 부족");

        service.processReport(9L, new ReportProcessDto(ReportStatus.RESOLVED, null), admin);
        assertThat(report.getRejectReason()).isNull(); // 반려 해제 시 사유 초기화
    }

    @Test
    void 내_신고_목록은_반려_사유를_포함해_내려간다() {
        report.process(ReportStatus.REJECTED); report.addRejectReason("증거 부족");
        when(reportRepository.findAllByUserUserIdOrderByCreatedAtDesc(1L, PageRequest.of(0, 5))).thenReturn(new PageImpl<>(List.of(report)));

        Page<ReportResponseDto> page = service.getMyReports(customer, 0, 5);

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).getStatus()).isEqualTo(ReportStatus.REJECTED);
        assertThat(page.getContent().get(0).getRejectReason()).isEqualTo("증거 부족");
        verify(reportRepository, never()).findAllReports(any()); // 전체 목록(관리자용)은 타지 않는다
    }
}
