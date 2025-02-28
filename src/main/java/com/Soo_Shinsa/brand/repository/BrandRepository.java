package com.Soo_Shinsa.brand.repository;

import com.Soo_Shinsa.brand.model.Brand;
import com.Soo_Shinsa.global.exception.NotFoundException;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

import static com.Soo_Shinsa.global.exception.ErrorCode.NOT_FOUND_BRAND;

public interface BrandRepository extends JpaRepository<Brand, Long>, BrandCustomRepository {

    List<Brand> findAllByUserUserId(Long userId);

    default Brand findByIdOrElseThrow(Long brandId) {
        return findById(brandId).orElseThrow(
                () -> new NotFoundException(NOT_FOUND_BRAND));
    }
}
