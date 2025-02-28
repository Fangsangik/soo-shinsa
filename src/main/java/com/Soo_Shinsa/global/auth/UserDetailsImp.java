package com.Soo_Shinsa.global.auth;

import com.Soo_Shinsa.user.model.User;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.ArrayList;
import java.util.Collection;

@Getter
@RequiredArgsConstructor
public class UserDetailsImp implements UserDetails {

    private final User user;

    /**
     * 계정의 권한 리스트를 리턴.
     *
     * @return {@code Collection<? extends GrantedAuthority>}
     */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {

        return new ArrayList<>(this.user.getRole().getAuthorities());
    }

    /**
     * 사용자의 자격 증명 반환.
     *
     * @return 암호
     */
    @Override
    public String getPassword() {
        return this.user.getPassword();
    }

    /**
     * 사용자 이름 반환.
     *
     * @return 사용자 이름
     */
    @Override
    public String getUsername() {
        return this.user.getEmail();
    }

    /**
     * 계정 만료.
     *
     * @return 사용 여부
     * @apiNote 사용하지 않을 경우 true를 리턴하도록 재정의.
     */
    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    /**
     * 계정 잠금.
     *
     * @return 사용 여부
     * @apiNote 사용하지 않을 경우 true를 리턴하도록 재정의.
     */
    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    /**
     * 자격 증명 만료.
     *
     * @return 사용 여부
     * @apiNote 사용하지 않을 경우 true를 리턴하도록 재정의.
     */
    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    /**
     * 계정 사용 가능 여부.
     *
     * @return 사용 여부
     * @apiNote 사용하지 않을 경우 true를 리턴하도록 재정의.
     */
    @Override
    public boolean isEnabled() {
        return true;
    }
}
