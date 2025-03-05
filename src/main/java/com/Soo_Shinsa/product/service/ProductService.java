package com.Soo_Shinsa.product.service;

import com.Soo_Shinsa.product.dto.*;
import com.Soo_Shinsa.user.model.User;
import org.springframework.data.domain.Page;
import org.springframework.web.multipart.MultipartFile;


public interface ProductService {

    ProductResponseDto createProduct(User user, ProductRequestDto dto, Long brandId, MultipartFile imageFile);

    ProductUpdateDto updateProduct(User user, ProductUpdateDto dto, Long productId, MultipartFile imageFile);

    FindProductResponseDto findProduct(Long productId);

    Page<ProductResponseDto> findAllProduct(Long brandId, FindProductRequestDto requestDto, int page, int size);

    Page<ProductResponseDto> findUserBasedRecommendation(User user, int page, int size);

    void deleteProduct(Long productId, User user);
}

