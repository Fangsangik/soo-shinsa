package com.Soo_Shinsa.report.repository;

import com.Soo_Shinsa.global.exception.NotFoundException;
import com.Soo_Shinsa.report.model.Report;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import static com.Soo_Shinsa.global.exception.ErrorCode.NOT_FOUND_REPORT;

public interface ReportRepository extends JpaRepository<Report, Long> {
    default Report findByIdOrElseThrow(Long reportId) {
        return findById(reportId).orElseThrow(() -> new NotFoundException(NOT_FOUND_REPORT));
    }
    @Query("SELECT r FROM Report r ORDER BY r.createdAt DESC")
    Page<Report> findAllReports(Pageable pageable);
}
