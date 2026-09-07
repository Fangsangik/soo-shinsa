package com.Soo_Shinsa.coupon.repository;

import com.Soo_Shinsa.coupon.model.Coupon;
import com.Soo_Shinsa.global.exception.ErrorCode;
import com.Soo_Shinsa.global.exception.NotFoundException;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface CouponRepository extends JpaRepository<Coupon, Long> {

    default Coupon findByIdOrElseThrow(Long couponId) {
        return findById(couponId).orElseThrow(
                () -> new NotFoundException(ErrorCode.NOT_FOUND_COUPON_COUNT)
        );
    }

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM Coupon c WHERE c.id = :id")
    Optional<Coupon> findByIdWithLock(@Param("id") Long id);

    /**
     * 선착순 정원 안에서만 발급 수를 올린다.
     * 정원 검사를 WHERE 로 내렸기 때문에 분산락 없이도 초과 발급이 불가능하다.
     *
     * @return 1이면 발급 성공, 0이면 정원 소진
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Coupon c SET c.issuedCount = c.issuedCount + 1 " +
           "WHERE c.id = :id AND c.issuedCount < c.maxCount")
    int increaseIssuedCount(@Param("id") Long id);

    /**
     * 쿠폰 사용 시 잔여 수량 차감. 0이면 남은 수량 없음.
     * remainingCount 도입 전 쿠폰은 값이 없으므로 정원으로 간주한다.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Coupon c SET c.remainingCount = COALESCE(c.remainingCount, c.maxCount) - :amount " +
           "WHERE c.id = :id AND COALESCE(c.remainingCount, c.maxCount) >= :amount")
    int decreaseRemainingCount(@Param("id") Long id, @Param("amount") int amount);
}
