package com.Soo_Shinsa.brand.repository;

import com.Soo_Shinsa.brand.model.Brand;
import com.Soo_Shinsa.global.constant.BrandStatus;
import com.Soo_Shinsa.global.exception.NotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

import static com.Soo_Shinsa.global.exception.ErrorCode.NOT_FOUND_BRAND;

public interface BrandRepository extends JpaRepository<Brand, Long>, BrandCustomRepository {

    List<Brand> findAllByUserUserId(Long userId);
    
    // 승인 관련 쿼리 메소드들
    @Query("SELECT b FROM Brand b WHERE b.status = :status ORDER BY b.createdAt ASC")
    Page<Brand> findByStatus(@Param("status") BrandStatus status, Pageable pageable);
    
    @Query("SELECT COUNT(b) FROM Brand b WHERE b.status = :status")
    long countByStatus(@Param("status") BrandStatus status);

    /** 쿠폰 수량 제한이 걸린 브랜드의 잔여 수량 차감. 0이면 소진. */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Brand b SET b.couponCount = b.couponCount - 1 " +
           "WHERE b.id = :id AND b.isCouponLimited = true AND b.couponCount > 0")
    int decreaseCouponCount(@Param("id") Long id);

    default Brand findByIdOrElseThrow(Long brandId) {
        return findById(brandId).orElseThrow(
                () -> new NotFoundException(NOT_FOUND_BRAND));
    }
}
