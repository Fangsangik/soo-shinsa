package com.Soo_Shinsa.user.model;

import java.math.BigDecimal;
import com.Soo_Shinsa.global.constant.Role;
import com.Soo_Shinsa.global.constant.UserStatus;
import com.Soo_Shinsa.user.dto.KakaoUserInfoResponseDto;
import com.Soo_Shinsa.user.dto.UserUpdateRequestDto;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long userId;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String phoneNum;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private UserStatus status;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private Role role;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_grade_id")
    private UserGrade userGrade;

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL)
    private KakaoUser kakaoUser;

    /** 포인트 잔액. 결제 승인 때 적립되고 주문 때 사용한다. */
    @Column(nullable = false)
    private BigDecimal point = BigDecimal.ZERO;

    /** 누적 구매액. 등급 승급 판정에 쓴다. */
    @Column(nullable = false)
    private BigDecimal totalPurchase = BigDecimal.ZERO;

    @Builder
    public User(String email, String password, String name, String phoneNum, UserStatus status, Role role, UserGrade userGrade, KakaoUser kakaoUser) {
        this.email = email;
        this.password = password;
        this.name = name;
        this.phoneNum = phoneNum;
        this.status = status;
        this.role = role;
        this.userGrade = userGrade;
        this.kakaoUser = kakaoUser;
    }

    public void updateUserGrade(UserGrade userGrade) {
        this.userGrade = userGrade;
    }

    public void addPoint(BigDecimal amount) {
        this.point = this.point.add(amount);
    }

    /** 잔액을 넘는 차감은 호출부에서 막는다. 방어적으로 0 밑으로는 내려가지 않게 한다. */
    public void subtractPoint(BigDecimal amount) {
        this.point = this.point.subtract(amount).max(BigDecimal.ZERO);
    }

    public void addPurchase(BigDecimal amount) {
        this.totalPurchase = this.totalPurchase.add(amount);
    }

    public void subtractPurchase(BigDecimal amount) {
        this.totalPurchase = this.totalPurchase.subtract(amount).max(BigDecimal.ZERO);
    }

    public void delete() {
        this.status = UserStatus.DELETED;
    }

    public void update(UserUpdateRequestDto userUpdateRequestDto) {
        // 보내지 않은 항목은 그대로 둔다. 예전에는 비밀번호만 바꿔도 이름/전화가 null 로 지워졌다.
        if (userUpdateRequestDto.getName() != null) {
            this.name = userUpdateRequestDto.getName();
        }
        if (userUpdateRequestDto.getPhoneNum() != null) {
            this.phoneNum = userUpdateRequestDto.getPhoneNum();
        }
    }
    public void updatePassword(String password) {
        this.password = password;
    }

    // 역할 검증 메소드
    public boolean isAdmin() {
        return Role.ADMIN.equals(this.role);
    }

    public boolean isVendor() {
        return Role.VENDOR.equals(this.role);
    }

    public boolean isAdminOrVendor() {
        return isAdmin() || isVendor();
    }

    public void assignKakaoUser(KakaoUser kakaoUser) {
        this.kakaoUser = kakaoUser;
        kakaoUser.assignUser(this);
    }

    public User(KakaoUserInfoResponseDto userInfo) {
        this.email = userInfo.getKakaoAccount().getEmail();}
}