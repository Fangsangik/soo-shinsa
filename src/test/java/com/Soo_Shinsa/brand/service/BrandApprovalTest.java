package com.Soo_Shinsa.brand.service;

import com.Soo_Shinsa.brand.dto.BrandApprovalDto;
import com.Soo_Shinsa.brand.model.Brand;
import com.Soo_Shinsa.brand.repository.BrandRepository;
import com.Soo_Shinsa.global.constant.BrandStatus;
import com.Soo_Shinsa.global.constant.Role;
import com.Soo_Shinsa.global.exception.InvalidInputException;
import com.Soo_Shinsa.global.exception.NoAuthorizedException;
import com.Soo_Shinsa.user.model.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;

/**
 * 브랜드 승인 경로의 권한/상태 가드 검증.
 * @PreAuthorize 나 URL 규칙이 사라져도 서비스 계층이 막아야 한다.
 */
@ExtendWith(MockitoExtension.class)
class BrandApprovalTest {

    @Mock
    private BrandRepository brandRepository;

    @InjectMocks
    private BrandServiceImpl brandService;

    private final BrandApprovalDto dto = new BrandApprovalDto("승인", "코멘트");

    private User user(Role role) {
        return User.builder().email("a@b.c").role(role).build();
    }

    private void stubBrand(BrandStatus status) {
        Brand brand = Brand.builder().name("brand").status(status).build();
        lenient().when(brandRepository.findByIdOrElseThrow(any())).thenReturn(brand);
    }

    @Test
    void 관리자가_아니면_승인할_수_없다() {
        stubBrand(BrandStatus.APPLY);
        assertThrows(NoAuthorizedException.class,
                () -> brandService.approveBrand(user(Role.VENDOR), 1L, dto));
        assertThrows(NoAuthorizedException.class,
                () -> brandService.approveBrand(user(Role.CUSTOMER), 1L, dto));
    }

    @Test
    void 신청_상태가_아닌_브랜드는_승인할_수_없다() {
        stubBrand(BrandStatus.OPEN);
        // 500이 아니라 400으로 떨어져야 한다
        assertThrows(InvalidInputException.class,
                () -> brandService.approveBrand(user(Role.ADMIN), 1L, dto));
    }
}
