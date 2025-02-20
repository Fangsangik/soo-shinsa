package com.Soo_Shinsa.user.service;

import com.Soo_Shinsa.auth.*;
import com.Soo_Shinsa.auth.dto.JwtAuthResponseDto;
import com.Soo_Shinsa.auth.dto.RefreshTokenRequestDto;
import com.Soo_Shinsa.constant.AuthenticationScheme;
import com.Soo_Shinsa.constant.GradeType;
import com.Soo_Shinsa.constant.Role;
import com.Soo_Shinsa.constant.UserStatus;
import com.Soo_Shinsa.exception.*;
import com.Soo_Shinsa.user.dto.*;
import com.Soo_Shinsa.user.model.Grade;
import com.Soo_Shinsa.user.model.User;
import com.Soo_Shinsa.user.model.UserGrade;
import com.Soo_Shinsa.user.repository.GradeRepository;
import com.Soo_Shinsa.user.repository.KakaoUserRepository;
import com.Soo_Shinsa.user.repository.UserGradeRepository;
import com.Soo_Shinsa.user.repository.UserRepository;
import com.Soo_Shinsa.utils.ResponseMessage;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.Soo_Shinsa.exception.ErrorCode.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final GradeRepository gradeRepository;
    private final UserGradeRepository userGradeRepository;
    private final AuthenticationManager authenticationManager;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;
    private final JwtAccessTokenService jwtAccessTokenService;
    private final JwtRefreshTokenService jwtRefreshTokenService;
    private final UserDetailsServiceImp userDetailsService;
    private final JwtBlackListService jwtBlackListService;
    private final KakaoUserRepository kakaoUserRepository;

    @Transactional
    @Override
    public UserResponseDto create(SignInRequestDto dto) {
        //검증
        //중복체크
        if (userRepository.existsByEmail(dto.getEmail())) {
            throw new NoAuthorizedException(EMAIL_EXIST);
        }

        //user 생성
        User user = dto.toEntity(passwordEncoder.encode(dto.getPassword()));

        //customer 경우 customer grade 생성
        if (user.getRole().equals(Role.CUSTOMER)) {
            user.updateUserGrade(createNewUserGrade());
        }

        //저장
        userRepository.save(user);

        return new UserResponseDto(user);
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

        log.info("🟢 AccessToken 생성 완료: {}", accessToken);

        jwtAccessTokenService.saveAccessToken(accessToken, user.getEmail(), jwtProvider.getExpiryMillis());

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
        if (!passwordEncoder.matches(userUpdateRequestDto.getOldPassword(), userById.getPassword())) {
            throw new NoAuthorizedException(WRONG_PASSWORD);
        }

        //user 업데이트
        userById.update(userUpdateRequestDto);
        userById.updatePassword(passwordEncoder.encode(userUpdateRequestDto.getNewPassword()));

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
        log.info("로그아웃 요청 처리 중: 토큰 = {}", token);

        // 블랙리스트 추가 및 토큰 삭제
        jwtBlackListService.addBlackList(token, jwtProvider.getExpiryMillis());
        jwtAccessTokenService.deleteAllAccessTokens();
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
