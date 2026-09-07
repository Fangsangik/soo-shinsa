package com.Soo_Shinsa.coupon.repository;

import com.Soo_Shinsa.coupon.model.Coupon;
import com.Soo_Shinsa.coupon.model.CouponUser;
import com.Soo_Shinsa.global.exception.NotFoundException;
import com.Soo_Shinsa.user.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

import static com.Soo_Shinsa.global.exception.ErrorCode.NOT_FOUND_CATEGORY;

@Repository
public interface CouponUserRepository extends JpaRepository<CouponUser, Long> {

    Optional<CouponUser> findByCouponIdAndUserUserId(Long couponId, Long userId);

    boolean existsByCouponAndUser(Coupon coupon, User user);

    /** 특정 쿠폰의 발급 건수. 전역 count 는 다른 데이터에 오염되므로 쓰지 않는다. */
    long countByCouponId(Long couponId);

    /**
     * 아직 쓰지 않은 경우에만 사용 처리한다.
     * @return 1이면 이번 호출이 사용 처리한 것, 0이면 이미 사용됨
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE CouponUser cu SET cu.isUsed = true, cu.usedAt = :usedAt " +
           "WHERE cu.id = :id AND cu.isUsed = false")
    int markAsUsed(@Param("id") Long id, @Param("usedAt") LocalDate usedAt);

    default CouponUser findByIdOrElseThrow(Long couponUserId) {
        return findById(couponUserId).orElseThrow(
                () -> new NotFoundException(NOT_FOUND_CATEGORY)
        );
    }
}
