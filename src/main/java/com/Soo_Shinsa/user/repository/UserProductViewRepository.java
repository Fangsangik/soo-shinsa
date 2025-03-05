package com.Soo_Shinsa.user.repository;

import com.Soo_Shinsa.user.model.UserProductView;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;


public interface UserProductViewRepository extends JpaRepository<UserProductView, Long> {

    @Query("SELECT upv FROM UserProductView upv WHERE upv.user.userId = :userId ORDER BY upv.viewDate DESC")
    List<UserProductView> findRecentlyViewedProductOptions(@Param("userId") Long userId, Pageable pageable);

}
