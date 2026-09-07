package com.Soo_Shinsa.user.dto;

import com.Soo_Shinsa.global.constant.GradeType;
import com.Soo_Shinsa.global.constant.Role;
import com.Soo_Shinsa.global.constant.UserStatus;
import com.Soo_Shinsa.user.model.User;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Getter
public class SignInRequestDto {
    @NotBlank(message = "이메일을 입력해주세요.")
    @Pattern(regexp = "^[\\w!#$%&'*+/=?`{|}~^.-]+@[\\w.-]+\\.[a-zA-Z]{2,6}$", message = "이메일 형식이 올바르지 않습니다.")
    private String email;

    @Pattern(regexp = "^\\d{2,3}-\\d{3,4}-\\d{4}$", message = "전화번호 형식이 올바르지 않습니다.")
    private String phoneNum;

    @NotBlank(message = "비밀번호를 입력해 주세요")
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&#])[A-Za-z\\d@$!%*?&#]{8,}$", message = "비밀번호 형식이 올바르지 않습니다. 8자 이상, 대소문자 포함, 숫자 및 특수문자(@$!%*?&#) 포함")
    private String password;

    @NotBlank(message = "이름을 입력해주세요.")
    private String name;

    // Role은 서버에서 결정 - 보안상 클라이언트에서 직접 설정 불가
    
    // Admin 가입용 특별 키 (선택사항)
    private String adminKey;
    
    // Vendor 가입용 사업자등록번호 (선택사항)
    private String businessNumber;

    private GradeType grade;

    public SignInRequestDto(String email, String phoneNum, String password, String name, String adminKey, String businessNumber, GradeType grade) {
        this.email = email;
        this.phoneNum = phoneNum;
        this.password = password;
        this.name = name;
        this.adminKey = adminKey;
        this.businessNumber = businessNumber;
        this.grade = grade;
    }

    public User toEntity(String password, Role determinedRole) {
        return User.builder()
                .email(email)
                .phoneNum(phoneNum)
                .password(password)
                .name(name)
                .status(UserStatus.ACTIVE)
                .role(determinedRole)
                .build();
    }
}
