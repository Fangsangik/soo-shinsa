package com.Soo_Shinsa.user.service;

import com.Soo_Shinsa.auth.JwtProvider;
import com.Soo_Shinsa.user.dto.KakaoUserInfoResponseDto;
import com.Soo_Shinsa.user.model.User;
import com.Soo_Shinsa.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    private final UserRepository userRepository; // ✅ Service 대신 Repository 직접 주입
    private final JwtProvider jwtProvider;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = new DefaultOAuth2UserService().loadUser(userRequest);
        Map<String, Object> attributes = oAuth2User.getAttributes();

        KakaoUserInfoResponseDto userInfo = KakaoUserInfoResponseDto.fromAttributes(attributes);

        var userEntity = userRepository.findByEmail(userInfo.getKakaoAccount().getEmail())
                .orElseGet(() -> userRepository.save(new User(userInfo)));

        String accessToken = jwtProvider.generateTokenBy(userEntity.getEmail(), jwtProvider.getExpiryMillis());
        String refreshToken = jwtProvider.generateTokenBy(userEntity.getEmail(), jwtProvider.getRefreshExpiryMillis());

        Map<String, Object> customAttributes = new HashMap<>(attributes);
        customAttributes.put("accessToken", accessToken);
        customAttributes.put("refreshToken", refreshToken);

        return new DefaultOAuth2User(
                Collections.singleton(new SimpleGrantedAuthority("ROLE_CUSTOMER")),
                customAttributes,
                "id"
        );
    }
}
