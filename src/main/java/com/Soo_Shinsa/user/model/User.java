package com.Soo_Shinsa.user.model;

import com.Soo_Shinsa.constant.Role;
import com.Soo_Shinsa.constant.UserStatus;
import com.Soo_Shinsa.user.dto.UserUpdateRequestDto;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
@Table(name = "users")
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

    @ManyToOne
    @JoinColumn(name = "user_grade_id")
    private UserGrade userGrade;

    /** 포인트 잔액. 결제 승인 시 (결제액 × 등급 적립률) 적립, 주문 생성 시 사용 */
    @Column(nullable = false, columnDefinition = "decimal(38,2) default 0")
    private java.math.BigDecimal point = java.math.BigDecimal.ZERO;

    /** 누적 구매액. 등급 승급 판정 기준 */
    @Column(nullable = false, columnDefinition = "decimal(38,2) default 0")
    private java.math.BigDecimal totalPurchase = java.math.BigDecimal.ZERO;

    @Builder
    public User(String email, String password, String name, String phoneNum, UserStatus status, Role role, UserGrade userGrade) {
        this.email = email;
        this.password = password;
        this.name = name;
        this.phoneNum = phoneNum;
        this.status = status;
        this.role = role;
        this.userGrade = userGrade;
    }

    /** JWT 클레임만으로 만든 인증 주체. 필터에서 매 요청 DB 조회를 없애기 위한 것이라 id/email/name/role 만 채워진다.
     *  비밀번호·전화번호·등급이 필요한 서비스는 userRepository.findByIdOrElseThrow(user.getUserId()) 로 다시 읽어야 한다. */
    public static User principal(Long userId, String email, String name, Role role) {
        User u = new User();
        u.userId = userId; u.email = email; u.name = name; u.role = role; u.status = UserStatus.ACTIVE;
        return u;
    }

    public void addPoint(java.math.BigDecimal amount) { this.point = nz(this.point).add(amount); }
    public void subtractPoint(java.math.BigDecimal amount) { this.point = nz(this.point).subtract(amount).max(java.math.BigDecimal.ZERO); }
    public void addPurchase(java.math.BigDecimal amount) { this.totalPurchase = nz(this.totalPurchase).add(amount); }
    public void subtractPurchase(java.math.BigDecimal amount) { this.totalPurchase = nz(this.totalPurchase).subtract(amount).max(java.math.BigDecimal.ZERO); }
    private static java.math.BigDecimal nz(java.math.BigDecimal v) { return v == null ? java.math.BigDecimal.ZERO : v; }

    public void updateUserGrade(UserGrade userGrade) {
        this.userGrade = userGrade;
    }

    public void delete() {
        this.status = UserStatus.DELETED;
    }

    public void update(UserUpdateRequestDto userUpdateRequestDto) {
        this.name = userUpdateRequestDto.getName();
        this.phoneNum = userUpdateRequestDto.getPhoneNum();
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
}