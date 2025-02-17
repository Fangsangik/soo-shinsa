package com.Soo_Shinsa.category.repository;

import com.Soo_Shinsa.category.model.SubCategory;
import com.Soo_Shinsa.exception.ErrorCode;
import com.Soo_Shinsa.exception.NotFoundException;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SubCategoryRepository extends JpaRepository<SubCategory, Long> {

    default SubCategory findByIdOrElseThrow(Long subCategoryId) {
        return findById(subCategoryId).orElseThrow(
                () -> new NotFoundException(ErrorCode.NOT_FOUND_SUB_CATEGORY));
    }
}
