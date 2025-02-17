package com.Soo_Shinsa.category.service;

import com.Soo_Shinsa.category.dto.CategoryRequestDto;
import com.Soo_Shinsa.category.dto.CategoryResponseDto;
import com.Soo_Shinsa.category.dto.CategoryUpdateRequestDto;
import com.Soo_Shinsa.category.dto.FindCategoryResponseDto;
import com.Soo_Shinsa.category.model.Category;
import com.Soo_Shinsa.category.repository.CategoryRepository;
import com.Soo_Shinsa.user.model.User;
import com.Soo_Shinsa.utils.EntityValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;

    @Transactional
    @Override
    public CategoryResponseDto create(User user, CategoryRequestDto dto) {

        EntityValidator.validateAdminAccess(user);

        Category savedCategory = Category.builder()
                .name(dto.getName())
                .build();

        Category newCategory = categoryRepository.save(savedCategory);

        return CategoryResponseDto.toDto(newCategory);

    }


    @Override
    public CategoryResponseDto findById(Long categoryId) {

        Category findCategory = categoryRepository.findByIdOrElseThrow(categoryId);

        return CategoryResponseDto.toDto(findCategory);
    }


    @Override
    public Page<FindCategoryResponseDto> findAll(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return categoryRepository.findAllCategories(pageable);
    }

    @Override
    public CategoryResponseDto update(User user, CategoryUpdateRequestDto dto, Long categoryId) {

        Category findCategory = categoryRepository.findByIdOrElseThrow(categoryId);
        findCategory.update(dto.getName());
        return CategoryResponseDto.toDto(findCategory);
    }
}
