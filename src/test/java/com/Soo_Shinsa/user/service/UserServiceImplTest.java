package com.Soo_Shinsa.user.service;

import com.Soo_Shinsa.auth.*;
import com.Soo_Shinsa.auth.dto.RefreshTokenRequestDto;
import com.Soo_Shinsa.constant.GradeType;
import com.Soo_Shinsa.constant.Role;
import com.Soo_Shinsa.constant.UserStatus;
import com.Soo_Shinsa.exception.*;
import com.Soo_Shinsa.user.dto.LoginRequestDto;
import com.Soo_Shinsa.user.dto.SignInRequestDto;
import com.Soo_Shinsa.user.dto.UserDetailResponseDto;
import com.Soo_Shinsa.user.model.Grade;
import com.Soo_Shinsa.user.model.User;
import com.Soo_Shinsa.user.repository.GradeRepository;
import com.Soo_Shinsa.user.repository.UserGradeRepository;
import com.Soo_Shinsa.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Optional;

import static com.Soo_Shinsa.support.Fixtures.user;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** 회원 가입/탈퇴/재가입, 토큰 갱신, 내 정보 조회 규칙. */
@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {
    @Mock UserRepository userRepository;
    @Mock GradeRepository gradeRepository;
    @Mock UserGradeRepository userGradeRepository;
    @Mock AuthenticationManager authenticationManager;
    @Mock PasswordEncoder passwordEncoder;
    @Mock JwtProvider jwtProvider;
    @Mock JwtAccessTokenService jwtAccessTokenService;
    @Mock JwtRefreshTokenService jwtRefreshTokenService;
    @Mock UserDetailsServiceImp userDetailsService;
    @Mock JwtBlackListService jwtBlackListService;
    @InjectMocks UserServiceImpl service;

    User me;

    @BeforeEach
    void setUp() {
        me = user(1L, Role.CUSTOMER);
        ReflectionTestUtils.setField(service, "idleMillis", 1_800_000L);
    }

    private SignInRequestDto signup(String email) { return new SignInRequestDto(email, "010-0000-0000", "Passw0rd!", "홍길동", Role.CUSTOMER, GradeType.ROOKIE); }

    @Test
    void 가입은_ROOKIE_등급을_만들고_비밀번호를_암호화한다() {
        when(userRepository.findByEmail("new@test.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("Passw0rd!")).thenReturn("$2a$hash");
        when(gradeRepository.findByName(GradeType.ROOKIE)).thenReturn(Optional.of(new Grade(GradeType.ROOKIE, BigDecimal.ONE, BigDecimal.TEN)));

        service.create(signup("new@test.com"));

        verify(userGradeRepository).save(any());
        verify(userRepository).save(argThat(u -> u.getPassword().equals("$2a$hash") && u.getStatus() == UserStatus.ACTIVE && u.getUserGrade() != null));
    }

    @Test
    void 중복_이메일과_탈퇴한_이메일은_다른_코드로_거절된다() {
        when(userRepository.findByEmail("me@test.com")).thenReturn(Optional.of(me));
        assertThatThrownBy(() -> service.create(signup("me@test.com"))).isInstanceOf(DuplicatedException.class).extracting("errorCode").isEqualTo(ErrorCode.EMAIL_EXIST);

        me.delete();
        assertThatThrownBy(() -> service.create(signup("me@test.com"))).isInstanceOf(DuplicatedException.class).extracting("errorCode").isEqualTo(ErrorCode.EMAIL_DELETED);
        verify(userRepository, never()).save(any());
    }

    @Test
    void 탈퇴는_DB_사용자를_다시_읽어_비밀번호를_확인하고_DELETED_로_바꾼다() {
        User principal = User.principal(1L, me.getEmail(), me.getName(), Role.CUSTOMER); // 필터가 만든 가벼운 주체 (비밀번호 없음)
        when(userRepository.findByIdOrElseThrow(1L)).thenReturn(me);
        when(passwordEncoder.matches("wrong", "pw")).thenReturn(false);
        when(passwordEncoder.matches("pw", "pw")).thenReturn(true);

        assertThatThrownBy(() -> service.leave("wrong", principal)).isInstanceOf(DuplicatedException.class);
        assertThat(me.getStatus()).isEqualTo(UserStatus.ACTIVE);

        service.leave("pw", principal);
        assertThat(me.getStatus()).isEqualTo(UserStatus.DELETED);
    }

    @Test
    void 탈퇴한_계정은_로그인할_수_없다() {
        me.delete();
        when(userRepository.findByEmailOrElseThrow(me.getEmail())).thenReturn(me);
        LoginRequestDto dto = new LoginRequestDto(); ReflectionTestUtils.setField(dto, "email", me.getEmail()); ReflectionTestUtils.setField(dto, "password", "pw");
        assertThatThrownBy(() -> service.login(dto)).isInstanceOf(NoAuthorizedException.class).extracting("errorCode").isEqualTo(ErrorCode.DELETED_USER);
        verifyNoInteractions(jwtProvider, jwtAccessTokenService);
    }

    @Test
    void 내_정보는_토큰_주체가_아니라_DB에서_읽는다() {
        User principal = User.principal(1L, me.getEmail(), "옛이름", Role.CUSTOMER);
        when(userRepository.findByIdOrElseThrow(1L)).thenReturn(me);
        UserDetailResponseDto dto = service.getUser(principal);
        assertThat(dto.getUserId()).isEqualTo(1L);
        assertThat(dto.getName()).isEqualTo(me.getName());
        assertThat(dto.getPhoneNum()).isEqualTo("010-0000-0000");
    }

    @Test
    void refresh_token_이_저장된_것과_다르면_거절하고_같으면_새_access_token_을_유휴_TTL_로_저장한다() {
        when(jwtProvider.getUsername("rt")).thenReturn(me.getEmail());
        when(jwtRefreshTokenService.getRefreshToken(me.getEmail())).thenReturn("other");
        assertThatThrownBy(() -> service.refreshAccessToken(new RefreshTokenRequestDto("rt", "old"))).isInstanceOf(InvalidInputException.class);

        when(jwtRefreshTokenService.getRefreshToken(me.getEmail())).thenReturn("rt");
        when(jwtProvider.getExpiryMillis()).thenReturn(3_600_000L);
        when(jwtProvider.generateTokenBy(me.getEmail(), 3_600_000L)).thenReturn("newAT");
        when(userDetailsService.loadUserByUsername(me.getEmail())).thenReturn(new UserDetailsImp(me));

        assertThat(service.refreshAccessToken(new RefreshTokenRequestDto("rt", "old")).getAccessToken()).isEqualTo("newAT");
        verify(jwtAccessTokenService).saveAccessToken("newAT", me.getEmail(), 1_800_000L);
    }
}
