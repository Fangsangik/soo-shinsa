package com.Soo_Shinsa.product.service;

import com.Soo_Shinsa.product.dto.*;
import com.Soo_Shinsa.user.model.User;
import org.springframework.data.domain.Page;

import java.util.List;
import org.springframework.web.multipart.MultipartFile;


public interface ProductService {

    ProductResponseDto createProduct(User user, ProductRequestDto dto, Long brandId, MultipartFile imageFile);

    ProductUpdateDto updateProduct(User user, ProductUpdateDto dto, Long productId, MultipartFile imageFile);

    FindProductResponseDto findProduct(Long productId);

    /** 상품 조회 + 조회 이력 기록. viewer 가 null 이면(비로그인) 기록하지 않는다. */
    FindProductResponseDto findProduct(Long productId, User viewer);

    Page<ProductResponseDto> findAllProduct(Long brandId, FindProductRequestDto requestDto, int page, int size);

    List<String> autocomplete(String keyword, int limit);

    /** 오늘 많이 찾은 검색어 */
    List<String> popularKeywords(int limit);

    Page<ProductResponseDto> findUserBasedRecommendation(User user, int page, int size);

    void deleteProduct(Long productId, User user);
}

