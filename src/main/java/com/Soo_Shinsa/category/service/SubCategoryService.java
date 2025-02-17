package com.Soo_Shinsa.category.service;

import com.Soo_Shinsa.category.dto.SubCategoryRequestDto;
import com.Soo_Shinsa.category.dto.SubCategoryResponseDto;
import com.Soo_Shinsa.category.dto.SubCategoryUpdateRequestDto;
import com.Soo_Shinsa.category.dto.SubCategoryUpdateResponseDto;
import com.Soo_Shinsa.user.model.User;

public interface SubCategoryService {

    SubCategoryResponseDto createSubCategory(User user, SubCategoryRequestDto dto);
    SubCategoryResponseDto findSubCategoryById(Long categoryId);
    SubCategoryUpdateResponseDto updateSubCategory(User user, SubCategoryUpdateRequestDto dto, Long subCategoryId);
}
