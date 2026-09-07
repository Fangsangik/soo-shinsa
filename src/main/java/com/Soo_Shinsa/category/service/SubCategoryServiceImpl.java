package com.Soo_Shinsa.category.service;

import com.Soo_Shinsa.category.dto.SubCategoryRequestDto;
import com.Soo_Shinsa.category.dto.SubCategoryResponseDto;
import com.Soo_Shinsa.category.dto.SubCategoryUpdateRequestDto;
import com.Soo_Shinsa.category.dto.SubCategoryUpdateResponseDto;
import com.Soo_Shinsa.category.model.Category;
import com.Soo_Shinsa.category.model.SubCategory;
import com.Soo_Shinsa.category.repository.CategoryRepository;
import com.Soo_Shinsa.category.repository.SubCategoryRepository;
import com.Soo_Shinsa.global.utils.EntityValidator;
import com.Soo_Shinsa.user.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SubCategoryServiceImpl implements SubCategoryService {

    private final SubCategoryRepository subCategoryRepository;
    private final CategoryRepository categoryRepository;

    /** 브랜드 등록 화면에서 서브 카테고리를 고르려면 목록이 필요하다. 조회는 공개. */
    @Transactional(readOnly = true)
    @Override
    public java.util.List<SubCategoryResponseDto> findAll(Long categoryId) {
        java.util.List<com.Soo_Shinsa.category.model.SubCategory> found =
                (categoryId == null)
                        ? subCategoryRepository.findAll()
                        : subCategoryRepository.findAllByCategoryId(categoryId);
        return found.stream().map(SubCategoryResponseDto::toDto).toList();
    }

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
