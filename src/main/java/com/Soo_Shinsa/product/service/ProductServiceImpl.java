package com.Soo_Shinsa.product.service;

import com.Soo_Shinsa.brand.model.Brand;
import com.Soo_Shinsa.brand.repository.BrandRepository;
import com.Soo_Shinsa.constant.TargetType;
import com.Soo_Shinsa.image.model.Image;
import com.Soo_Shinsa.image.service.ImageService;
import com.Soo_Shinsa.order.repository.OrderItemRepository;
import com.Soo_Shinsa.product.dto.*;
import com.Soo_Shinsa.product.model.Product;
import com.Soo_Shinsa.product.model.ProductOption;
import com.Soo_Shinsa.product.repository.ProductOptionRepository;
import com.Soo_Shinsa.product.repository.ProductRepository;
import com.Soo_Shinsa.review.repository.ReviewRepository;
import com.Soo_Shinsa.user.model.User;
import com.Soo_Shinsa.user.repository.UserRepository;
import com.Soo_Shinsa.utils.EntityValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final BrandRepository brandRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final ImageService imageService;
    private final ProductOptionRepository productOptionRepository;
    private final ReviewRepository reviewRepository;
    private final OrderItemRepository orderItemRepository;


    @Transactional
    @Override
    public ProductResponseDto createProduct(User user, ProductRequestDto dto, Long brandId, MultipartFile imageFile) {

        User userById = userRepository.findByIdOrElseThrow(user.getUserId());
        Brand brand = brandRepository.findByIdOrElseThrow(brandId);

        EntityValidator.validateAdminOrVendorAccess(userById);

        String imageUrl = null;
        if (imageFile != null && !imageFile.isEmpty()) {
            Image uploaded = imageService.uploadImage(imageFile, TargetType.PRODUCT, null);
            imageUrl = uploaded.getPath();
        }

        Product product = Product.builder()
                .price(dto.getPrice())
                .name(dto.getName())
                .productStatus(dto.getStatus())
                .brand(brand)
                .imageUrl(imageUrl)
                .build();

        Product savedProduct = productRepository.save(product);

        return ProductResponseDto.toDto(savedProduct);
    }



    @Transactional
    @Override
    public ProductUpdateDto updateProduct(User user, ProductUpdateDto dto, Long productId, MultipartFile imageFile) {


        Product product = productRepository.findByIdOrElseThrow(productId);

        EntityValidator.validateAdminOrVendorAccess(user);

        String newImageUrl = product.getImageUrl(); // 기존 이미지 URL 유지
        if (imageFile != null && !imageFile.isEmpty()) {
            // 기존 이미지 삭제 후 새로운 이미지 업로드
            Image updatedImage = imageService.updateImage(imageFile, product.getImageUrl(), TargetType.PRODUCT);
            newImageUrl = updatedImage.getPath();
        }

        product.update(dto.getName(), dto.getPrice(), dto.getStatus(), newImageUrl);

        return ProductUpdateDto.toDto(product);
    }

    @Override
    public FindProductResponseDto findProduct(Long productId) {

        Product product = productRepository.findByIdOrElseThrow(productId);

        List<ProductOption> productOptions = productOptionRepository.findProductOptionByProductId(productId);

        return FindProductResponseDto.toDto(product, productOptions);
    }

    @Override
    public Page<ProductResponseDto> findAllProduct(Long brandId, FindProductRequestDto requestDto, int page, int size) {
         Pageable pageable = PageRequest.of(page, size);
         return productRepository.findAllProduct(brandId, requestDto, pageable);
    }

    @Transactional
    public void deleteProduct(Long productId, User user) {
        EntityValidator.validateAdminOrVendorAccess(user);

        Product product = productRepository.findByIdOrElseThrow(productId);

        reviewRepository.deleteAllByProductId(productId);
        orderItemRepository.deleteAllByProductId(productId);
        productOptionRepository.deleteAllByProductId(productId);

        productRepository.delete(product);
    }


}
