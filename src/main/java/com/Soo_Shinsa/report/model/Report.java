package com.Soo_Shinsa.report.model;

import com.Soo_Shinsa.global.constant.BaseTimeEntity;
import com.Soo_Shinsa.global.constant.ReportStatus;
import com.Soo_Shinsa.global.constant.TargetType;
import com.Soo_Shinsa.user.model.User;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
public class Report extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long targetId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TargetType targetType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReportStatus status;

    @Column(nullable = false)
    private String content;

    private String rejectReason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Builder
    public Report(Long targetId, TargetType targetType, ReportStatus status, String content, String rejectReason, User user) {
        this.targetId = targetId;
        this.targetType = targetType;
        this.status = status;
        this.content = content;
        this.rejectReason = rejectReason;
        this.user = user;
    }


    public void process(ReportStatus status) {
        if (status == null) {
            throw new IllegalArgumentException("status는 필수 입니다.");
        }

        // 상태 전환이 가능한 상태만 처리
        switch (status) {
            case OPEN:
            case IN_PROGRESS:
            case APPROVED:
            case REJECTED:
            case RESOLVED:
                this.status = status;
                break; // 유효한 상태는 그대로 진행
            default:
                throw new IllegalArgumentException("status는 OPEN, IN_PROGRESS, APPROVED, REJECTED, RESOLVED 중 하나여야 합니다.");
        }
    }


    public void addRejectReason(String rejectReason) {
        if (ReportStatus.REJECTED.equals(this.status) && rejectReason == null) {
            throw new IllegalArgumentException("rejectReason은 필수 입니다.");
        }

        this.rejectReason = rejectReason;
    }
}
