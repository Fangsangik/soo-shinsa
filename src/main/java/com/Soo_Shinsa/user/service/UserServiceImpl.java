package com.Soo_Shinsa.user.service;

import com.Soo_Shinsa.auth.JwtProvider;
import com.Soo_Shinsa.auth.dto.JwtAuthResponseDto;
import com.Soo_Shinsa.constant.AuthenticationScheme;
import com.Soo_Shinsa.constant.GradeType;
import com.Soo_Shinsa.constant.Role;
import com.Soo_Shinsa.constant.UserStatus;
import com.Soo_Shinsa.exception.DuplicatedException;
import com.Soo_Shinsa.exception.NoAuthorizedException;
import com.Soo_Shinsa.exception.NotFoundException;
import com.Soo_Shinsa.user.dto.*;
import com.Soo_Shinsa.user.model.Grade;
import com.Soo_Shinsa.user.model.User;
import com.Soo_Shinsa.user.model.UserGrade;
import com.Soo_Shinsa.user.repository.GradeRepository;
import com.Soo_Shinsa.user.repository.UserGradeRepository;
import com.Soo_Shinsa.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.Soo_Shinsa.exception.ErrorCode.*;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final GradeRepository gradeRepository;
    private final UserGradeRepository userGradeRepository;
    private final AuthenticationManager authenticationManager;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;

    @Transactional
    @Override
    public UserResponseDto create(SignInRequestDto dto) {
        //검증
        //중복체크
        if(userRepository.existsByEmail(dto.getEmail())){
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

    @Transactional(readOnly = true)
    @Override
    public JwtAuthResponseDto login(LoginRequestDto dto) {
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
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        dto.getEmail(),
                        dto.getPassword()
                )
        );

        //security context에 저장
        SecurityContextHolder.getContext().setAuthentication(auth);

        //토큰 생성
        String accessToken = jwtProvider.generateToken(auth);
        String refreshToken = jwtProvider.generateRefreshToken(user.getEmail());
        Long refreshTokenExpiration = jwtProvider.getRefreshExpiryMillis();

        return new JwtAuthResponseDto(AuthenticationScheme.BEARER.getName(), refreshToken, refreshTokenExpiration, accessToken, user.getName());
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
