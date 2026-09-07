package com.Soo_Shinsa.brand.service;

import com.Soo_Shinsa.brand.dto.*;
import com.Soo_Shinsa.brand.model.Brand;
import com.Soo_Shinsa.brand.repository.BrandRepository;
import com.Soo_Shinsa.category.model.SubCategory;
import com.Soo_Shinsa.category.repository.SubCategoryRepository;
import com.Soo_Shinsa.global.constant.BrandStatus;
import com.Soo_Shinsa.global.constant.Role;
import com.Soo_Shinsa.global.exception.InvalidInputException;
import com.Soo_Shinsa.global.exception.NoAuthorizedException;
import com.Soo_Shinsa.global.utils.EntityValidator;
import com.Soo_Shinsa.user.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.Soo_Shinsa.global.exception.ErrorCode.INVALID_BRAND_STATUS;
import static com.Soo_Shinsa.global.exception.ErrorCode.NO_AUTHORITY;

@Service
@RequiredArgsConstructor
public class BrandServiceImpl implements BrandService {

    private final BrandRepository brandRepository;
    private final SubCategoryRepository subCategoryRepository;

    @Transactional
    @Override
    public BrandResponseDto create(User user, BrandRequestDto dto) {
        // VENDOR만 브랜드 신청 가능하도록 제한 (ADMIN은 직접 승인 프로세스 사용)
        EntityValidator.validateAdminOrVendorAccess(user);

        SubCategory subCategory = subCategoryRepository.findByIdOrElseThrow(dto.getSubCategoryId());

        Brand savedBrand = Brand.builder()
                .subCategory(subCategory)
                .registrationNum(dto.getRegistrationNum())
                .name(dto.getName())
                .context(dto.getContext())
                .status(BrandStatus.APPLY) // 항상 신청 상태로 생성
                .user(user)
                .build();

        brandRepository.save(savedBrand);

        return BrandResponseDto.toDto(savedBrand);
    }

    @Transactional
    @Override
    public BrandUpdateResponseDto update(User user, BrandUpdateRequestDto dto, Long brandId) {

        EntityValidator.validateAdminOrVendorAccess(user);
        Brand findBrand = brandRepository.findByIdOrElseThrow(brandId);
        findBrand.update(dto.getRegistrationNum(), dto.getName(), dto.getContext(), dto.getStatus());

        return BrandUpdateResponseDto.toDto(findBrand);
    }

    @Override
    public BrandResponseDto findBrandById(Long brandId) {

        Brand findBrand = brandRepository.findByIdOrElseThrow(brandId);

        return BrandResponseDto.toDto(findBrand);
    }


    @Override
    public List<BrandResponseDto> getAllByUserId(User user) {

        List<Brand> brands = brandRepository.findAllByUserUserId(user.getUserId());

        return brands.stream().map(BrandResponseDto::toDto).toList();
    }


    @Override
    public Page<FindBrandAllResponseDto> getAll(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return brandRepository.getAllBrand(pageable);
    }

    // Admin 승인 관련 메소드 구현
    @Transactional
    @Override
    public BrandResponseDto approveBrand(User admin, Long brandId, BrandApprovalDto approvalDto) {
        // 1. Admin 권한 검증
        validateAdminRole(admin);
        
        // 2. Brand 조회 및 상태 검증
        Brand brand = brandRepository.findByIdOrElseThrow(brandId);
        validateBrandStatus(brand, BrandStatus.APPLY);
        
        // 3. 승인 처리
        brand.approve(admin.getUserId(), approvalDto.getApprovalReason(), approvalDto.getAdminComment());
        Brand savedBrand = brandRepository.save(brand);
        
        return BrandResponseDto.toDto(savedBrand);
    }

    @Transactional
    @Override
    public BrandResponseDto rejectBrand(User admin, Long brandId, BrandRejectionDto rejectionDto) {
        // 1. Admin 권한 검증
        validateAdminRole(admin);
        
        // 2. Brand 조회 및 상태 검증
        Brand brand = brandRepository.findByIdOrElseThrow(brandId);
        validateBrandStatus(brand, BrandStatus.APPLY);
        
        // 3. 거절 처리
        brand.reject(admin.getUserId(), rejectionDto.getRejectionReason(), rejectionDto.getAdminComment());
        Brand savedBrand = brandRepository.save(brand);
        
        return BrandResponseDto.toDto(savedBrand);
    }

    @Override
    public Page<BrandResponseDto> getPendingBrands(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Brand> pendingBrands = brandRepository.findByStatus(BrandStatus.APPLY, pageable);
        return pendingBrands.map(BrandResponseDto::toDto);
    }

    // 유틸리티 메소드들
    private void validateAdminRole(User user) {
        if (!Role.ADMIN.equals(user.getRole())) {
            throw new NoAuthorizedException(NO_AUTHORITY);
        }
    }

    private void validateBrandStatus(Brand brand, BrandStatus expectedStatus) {
        if (!expectedStatus.equals(brand.getStatus())) {
            throw new InvalidInputException(INVALID_BRAND_STATUS);
        }
    }

}
