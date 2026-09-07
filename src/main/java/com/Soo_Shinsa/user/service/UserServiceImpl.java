package com.Soo_Shinsa.user.service;

import com.Soo_Shinsa.global.auth.*;
import com.Soo_Shinsa.global.auth.dto.JwtAuthResponseDto;
import com.Soo_Shinsa.global.auth.dto.RefreshTokenRequestDto;
import com.Soo_Shinsa.global.constant.AuthenticationScheme;
import com.Soo_Shinsa.global.constant.GradeType;
import com.Soo_Shinsa.global.constant.Role;
import com.Soo_Shinsa.global.constant.UserStatus;
import com.Soo_Shinsa.global.exception.*;
import com.Soo_Shinsa.global.security.SecurityLogger;
import com.Soo_Shinsa.global.utils.ResponseMessage;
import com.Soo_Shinsa.user.dto.*;
import com.Soo_Shinsa.user.model.Grade;
import com.Soo_Shinsa.user.model.User;
import com.Soo_Shinsa.user.model.UserGrade;
import com.Soo_Shinsa.user.repository.GradeRepository;
import com.Soo_Shinsa.user.repository.KakaoUserRepository;
import com.Soo_Shinsa.user.repository.UserGradeRepository;
import com.Soo_Shinsa.user.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.Soo_Shinsa.global.exception.ErrorCode.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final GradeRepository gradeRepository;
    private final UserGradeRepository userGradeRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;
    private final JwtAccessTokenService jwtAccessTokenService;
    private final JwtRefreshTokenService jwtRefreshTokenService;
    private final UserDetailsServiceImp userDetailsService;
    private final JwtBlackListService jwtBlackListService;
    private final KakaoUserRepository kakaoUserRepository;
    
    @Value("${app.admin.secret-key}")
    private String adminSecretKey;

    @Transactional
    @Override
    public UserResponseDto create(SignInRequestDto dto) {
        // 1. 이메일 중복 체크
        if (userRepository.existsByEmail(dto.getEmail())) {
            throw new DuplicatedException(ErrorCode.EMAIL_EXIST);
        }

        // 2. 역할 결정 로직 (보안 강화)
        Role determinedRole = determineUserRole(dto);
        
        // 3. User 생성
        User user = dto.toEntity(passwordEncoder.encode(dto.getPassword()), determinedRole);

        // 4. Customer인 경우 등급 생성
        if (user.getRole().equals(Role.CUSTOMER)) {
            user.updateUserGrade(createNewUserGrade());
        }

        // 5. 저장
        userRepository.save(user);
        
        log.info("🟢 새 사용자 생성 완료: {} - 역할: {}", user.getEmail(), user.getRole());
        return new UserResponseDto(user);
    }
    
    /**
     * 사용자 역할을 안전하게 결정하는 메서드
     */
    private Role determineUserRole(SignInRequestDto dto) {
        // Admin Key 검증
        if (dto.getAdminKey() != null && !dto.getAdminKey().trim().isEmpty()) {
            if (adminSecretKey.equals(dto.getAdminKey())) {
                log.warn("🔑 관리자 계정 생성됨: {}", dto.getEmail());
                return Role.ADMIN;
            } else {
                throw new NoAuthorizedException(ErrorCode.NO_AUTHORITY);
            }
        }
        
        // 사업자등록번호로 Vendor 판단
        if (dto.getBusinessNumber() != null && !dto.getBusinessNumber().trim().isEmpty()) {
            // TODO: 실제로는 사업자등록번호 검증 API 호출
            if (isValidBusinessNumber(dto.getBusinessNumber())) {
                log.info("🏬 업주 계정 생성됨: {}", dto.getEmail());
                return Role.VENDOR;
            } else {
                throw new InvalidInputException(ErrorCode.WRONG_REQUEST);
            }
        }
        
        // 기본값: 일반 고객
        return Role.CUSTOMER;
    }
    
    /**
     * 사업자등록번호 유효성 검사 (간단한 형식 체크)
     */
    private boolean isValidBusinessNumber(String businessNumber) {
        // 간단한 형식 체크: XXX-XX-XXXXX
        return businessNumber.matches("^\\d{3}-\\d{2}-\\d{5}$");
    }

    @Transactional
    @Override
    public UserResponseDto findOrCreateKakaoUser(KakaoUserInfoResponseDto kakaoUserInfo) {
        return userRepository.findByEmail(kakaoUserInfo.getKakaoAccount().getEmail())
                .map(UserResponseDto::new) // 이미 존재하는 유저는 그대로 반환
                .orElseGet(() -> {
                    UserCreateRequestDto userDto = UserCreateRequestDto.builder()
                            .email(kakaoUserInfo.getKakaoAccount().getEmail())
                            .password(passwordEncoder.encode("KAKAO_DEFAULT_PASSWORD")) // 기본 패스워드 설정
                            .name(kakaoUserInfo.getKakaoAccount().getProfile().getNickname())
                            .phoneNum("010-1234-5678") // 기본값
                            .role(Role.CUSTOMER)
                            .build();

                    User savedUser = userRepository.save(userDto.toEntity());

                    KakaoUserCreateRequestDto kakaoUserDto = KakaoUserCreateRequestDto.builder()
                            .kakaoId(kakaoUserInfo.getId())
                            .email(kakaoUserInfo.getKakaoAccount().getEmail())
                            .nickname(kakaoUserInfo.getKakaoAccount().getProfile().getNickname())
                            .user(savedUser)
                            .build();

                    kakaoUserRepository.save(kakaoUserDto.toEntity());

                    return new UserResponseDto(savedUser);
                });
    }

    @Transactional
    @Override
    public JwtAuthResponseDto login(LoginRequestDto dto) {
        log.info("🟢 login 메서드 실행됨: {}", dto.getEmail());

        //사용자 확인
        User user = userRepository.findByEmailOrElseThrow(dto.getEmail());

        if (user.getStatus().equals(UserStatus.DELETED)) {
            throw new NoAuthorizedException(DELETED_USER);
        }

        //비밀번호 확인
        if (!passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
            throw new NoAuthorizedException(WRONG_PASSWORD);
        }

        //인증 객체를 저장
        UserDetails userDetails = new UserDetailsImp(user);
        Authentication auth = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);


        //security context에 저장
        SecurityContextHolder.getContext().setAuthentication(auth);

        //토큰 생성
        String accessToken = jwtProvider.generateTokenBy(user.getEmail(), jwtProvider.getExpiryMillis());
        String refreshToken = jwtProvider.generateTokenBy(user.getEmail(), jwtProvider.getRefreshExpiryMillis());

        log.info("🟢 AccessToken 생성 완료: {}", SecurityLogger.maskToken(accessToken));

        jwtAccessTokenService.saveAccessToken(accessToken, user.getEmail(), jwtProvider.getExpiryMillis());
        // 갱신 때 이 저장값과 비교한다. 저장하는 곳이 없어서 /users/refresh 가 항상
        // "유효하지 않은 토큰"으로 실패하고 있었다.
        jwtRefreshTokenService.saveRefreshToken(user.getEmail(), refreshToken, jwtProvider.getRefreshExpiryMillis());

        return new JwtAuthResponseDto(AuthenticationScheme.BEARER.getName(), refreshToken, jwtProvider.getRefreshExpiryMillis(), accessToken, user.getEmail());
    }

    @Override
    public UserDetailResponseDto getUser(User user) {
        return new UserDetailResponseDto(user);
    }

    @Transactional
    @Override
    public UserDetailResponseDto updateUser(User user, UserUpdateRequestDto userUpdateRequestDto) {
        //user 검증
        User userById = userRepository.findByIdOrElseThrow(user.getUserId());

        // 비밀번호 변경은 선택이다. 새 비밀번호를 보냈을 때만 기존 비밀번호를 확인하고 바꾼다.
        String newPassword = userUpdateRequestDto.getNewPassword();
        boolean changingPassword = newPassword != null && !newPassword.isBlank();
        if (changingPassword
                && !passwordEncoder.matches(userUpdateRequestDto.getOldPassword(), userById.getPassword())) {
            throw new NoAuthorizedException(WRONG_PASSWORD);
        }

        //user 업데이트
        userById.update(userUpdateRequestDto);
        if (changingPassword) {
            userById.updatePassword(passwordEncoder.encode(newPassword));
        }

        return new UserDetailResponseDto(userById);

    }


    @Transactional
    public void logout(HttpServletRequest request) {
        // SecurityContextHolder에서 인증 정보 가져오기
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !(authentication.getPrincipal() instanceof UserDetailsImp)) {
            log.warn("로그아웃 실패: 인증 정보 없음");
            throw new RuntimeException(ResponseMessage.AUTHENTICATION_REQUIRED);
        }

        UserDetailsImp userDetailsImp = (UserDetailsImp) authentication.getPrincipal();
        log.info("로그아웃 요청: 사용자 이메일 = {}", userDetailsImp.getUsername());

        // 헤더에서 Authorization 토큰 가져오기
        String token = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (token == null || !token.startsWith("Bearer ")) {
            log.warn("로그아웃 실패: 유효하지 않은 토큰");
            throw new RuntimeException(ResponseMessage.INVALID_TOKEN);
        }

        token = token.substring(7).trim(); // "Bearer " 제거
        log.info("로그아웃 요청 처리 중: 토큰 = {}", SecurityLogger.maskToken(token));

        // 블랙리스트 추가 및 현재 사용자의 토큰만 삭제
        jwtBlackListService.addBlackList(token, jwtProvider.getExpiryMillis());
        jwtAccessTokenService.deleteAccessToken(userDetailsImp.getUsername()); // 현재 사용자 이메일로 토큰 삭제
        jwtRefreshTokenService.deleteRefreshToken(userDetailsImp.getUsername());

        log.info("사용자 로그아웃 성공: {}", userDetailsImp.getUsername());
        SecurityContextHolder.clearContext();
    }

    @Transactional
    @Override
    public JwtAuthResponseDto refreshAccessToken(RefreshTokenRequestDto requestDto) {
        String refreshToken = requestDto.getRefreshToken();
        String email = jwtProvider.getUsername(refreshToken);  // username 대신 email 사용
        String storedRefreshToken = jwtRefreshTokenService.getRefreshToken(email);

        if (!refreshToken.equals(storedRefreshToken)) {
            throw new InvalidInputException(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        // 새로운 Access Token 생성
        String newAccessToken = jwtProvider.generateTokenBy(email, jwtProvider.getExpiryMillis());

        // Redis에 Access Token 저장
        jwtAccessTokenService.saveAccessToken(newAccessToken, email, jwtProvider.getExpiryMillis());

        // SecurityContext에 새로운 인증 정보 반영
        UserDetails userDetails = userDetailsService.loadUserByUsername(email);
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities()
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);

        // 새 Access Token 포함한 응답 생성
        return JwtAuthResponseDto.builder()
                .tokenAuthScheme(AuthenticationScheme.BEARER.getName())
                .refreshToken(refreshToken)
                .refreshTokenExpiration(jwtProvider.getRefreshExpiryMillis())
                .accessToken(newAccessToken)
                .email(email)
                .build();
    }

    @Transactional
    @Override
    public void leave(String password, User user) {
        //비밀번호 확인
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new DuplicatedException(DELETED_USER);
        }

        //탈퇴
        user.delete();
        userRepository.save(user);
    }

    private UserGrade createNewUserGrade() {
        // Grade 검증
        Grade grade = gradeRepository.findByName(GradeType.ROOKIE)
                .orElseThrow(() -> new NotFoundException(WRONG_REQUEST));

        // UserGrade 생성
        UserGrade userGrade = new UserGrade(grade);

        // 저장
        userGradeRepository.save(userGrade);
        return userGrade;
    }
}
