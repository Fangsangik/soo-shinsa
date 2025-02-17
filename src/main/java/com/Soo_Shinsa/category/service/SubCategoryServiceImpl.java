package com.Soo_Shinsa.category.service;

import com.Soo_Shinsa.category.dto.SubCategoryRequestDto;
import com.Soo_Shinsa.category.dto.SubCategoryResponseDto;
import com.Soo_Shinsa.category.dto.SubCategoryUpdateRequestDto;
import com.Soo_Shinsa.category.dto.SubCategoryUpdateResponseDto;
import com.Soo_Shinsa.category.model.Category;
import com.Soo_Shinsa.category.model.SubCategory;
import com.Soo_Shinsa.category.repository.CategoryRepository;
import com.Soo_Shinsa.category.repository.SubCategoryRepository;
import com.Soo_Shinsa.user.model.User;
import com.Soo_Shinsa.utils.EntityValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SubCategoryServiceImpl implements SubCategoryService {

    private final SubCategoryRepository subCategoryRepository;
    private final CategoryRepository categoryRepository;

    @Transactional
    @Override
    public SubCategoryResponseDto createSubCategory(User user, SubCategoryRequestDto dto) {
        EntityValidator.validateAdminAccess(user);

        Category category = categoryRepository.findByIdOrElseThrow(dto.getParentId());

        SubCategory subCategory = SubCategory.builder()
                .category(category)
                .name(dto.getName())
                .build();

        SubCategory newSubCategory = subCategoryRepository.save(subCategory);

        return SubCategoryResponseDto.toDto(newSubCategory);
    }

    @Override
    public SubCategoryResponseDto findSubCategoryById(Long categoryId) {
        SubCategory findSubCategory = subCategoryRepository.findByIdOrElseThrow(categoryId);

        return SubCategoryResponseDto.toDto(findSubCategory);
    }


    @Transactional
    @Override
    public SubCategoryUpdateResponseDto updateSubCategory(User user, SubCategoryUpdateRequestDto dto, Long subCategoryId) {
        EntityValidator.validateAdminAccess(user);

        SubCategory findSubCategory = subCategoryRepository.findByIdOrElseThrow(subCategoryId);

        findSubCategory.update(dto.getName());

        return SubCategoryUpdateResponseDto.toDto(findSubCategory);
    }
}
