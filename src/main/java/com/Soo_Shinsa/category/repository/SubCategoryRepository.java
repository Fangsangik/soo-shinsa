package com.Soo_Shinsa.category.repository;

import com.Soo_Shinsa.category.model.SubCategory;
import com.Soo_Shinsa.global.exception.ErrorCode;
import com.Soo_Shinsa.global.exception.NotFoundException;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SubCategoryRepository extends JpaRepository<SubCategory, Long> {

    List<SubCategory> findAllByCategoryId(Long categoryId);

    default SubCategory findByIdOrElseThrow(Long subCategoryId) {
        return findById(subCategoryId).orElseThrow(
                () -> new NotFoundException(ErrorCode.NOT_FOUND_SUB_CATEGORY));
    }
}
