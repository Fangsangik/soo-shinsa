package com.Soo_Shinsa.user.repository;

import com.Soo_Shinsa.exception.NotFoundException;
import com.Soo_Shinsa.user.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

import static com.Soo_Shinsa.exception.ErrorCode.NOT_FOUND_USER;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);

    @Query("SELECT u FROM User u " +
            "LEFT JOIN FETCH u.kakaoUser " +
            "LEFT JOIN FETCH u.userGrade " +
            "WHERE u.email = :email")
    Optional<User> findUserWithKakaoByEmail(@Param("email") String email);

    default User findByEmailOrElseThrow(String email) {
        return findUserWithKakaoByEmail(email)
                .orElseThrow(() -> new NotFoundException(NOT_FOUND_USER));
    }

    default User findByIdOrElseThrow(Long userId){
        return findById(userId).orElseThrow(()-> new NotFoundException(NOT_FOUND_USER));
    }

    Boolean existsByEmail(String email);
}
