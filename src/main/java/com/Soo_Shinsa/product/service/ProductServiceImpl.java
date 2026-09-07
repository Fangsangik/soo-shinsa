package com.Soo_Shinsa.product.service;

import com.Soo_Shinsa.brand.model.Brand;
import com.Soo_Shinsa.brand.repository.BrandRepository;
import com.Soo_Shinsa.global.constant.TargetType;
import com.Soo_Shinsa.global.utils.EntityValidator;
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
import com.Soo_Shinsa.user.model.UserProductView;
import com.Soo_Shinsa.user.repository.UserProductViewRepository;
import com.Soo_Shinsa.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final SearchKeywordRanking searchKeywordRanking;

    private final BrandRepository brandRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final ImageService imageService;
    private final ProductOptionRepository productOptionRepository;
    private final ReviewRepository reviewRepository;
    private final OrderItemRepository orderItemRepository;
    private final UserProductViewRepository userProductViewRepository;


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
        return findProduct(productId, null);
    }

    @Transactional
    @Override
    public FindProductResponseDto findProduct(Long productId, User viewer) {

        Product product = productRepository.findByIdOrElseThrow(productId);

        List<ProductOption> productOptions = productOptionRepository.findProductOptionByProductId(productId);

        // 조회 이력을 아무도 기록하지 않아 추천이 늘 랜덤으로 빠지고 있었다
        if (viewer != null) {
            userProductViewRepository.save(new UserProductView(viewer, product, LocalDate.now()));
        }

        return FindProductResponseDto.toDto(product, productOptions);
    }

    /**
     * 상품명 자동완성. 키워드가 2글자 미만이면 빈 목록 (LIKE '%a%' 전체 스캔 방지).
     */
    @Override
    public List<String> autocomplete(String keyword, int limit) {
        if (keyword == null || keyword.trim().length() < 2) {
            return List.of();
        }
        int capped = Math.min(Math.max(limit, 1), 20);
        String raw = keyword.trim();
        // BOOLEAN MODE 는 + - * " ( ) ~ < > @ 를 연산자로 해석한다.
        // 따옴표로 감싸 구문 검색으로 만들고, 입력에 든 따옴표는 제거한다.
        String phrase = "\"" + raw.replace("\"", " ") + "\"";
        return productRepository.findNameSuggestions(phrase, raw, capped);
    }

    @Override
    public Page<ProductResponseDto> findAllProduct(Long brandId, FindProductRequestDto requestDto, int page, int size) {
        // 자동완성은 키 입력마다 호출되므로 집계하지 않고, 실제 검색만 센다
        if (requestDto != null) {
            searchKeywordRanking.record(requestDto.getNameKeyword());
        }
        Pageable pageable = PageRequest.of(page, size);
        return productRepository.findAllProduct(brandId, requestDto, pageable);
    }

    @Override
    public List<String> popularKeywords(int limit) {
        return searchKeywordRanking.popularKeywords(limit);
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

    // DTO 변환이 lazy 연관(brand)을 건드리므로 세션이 필요하다
    @Transactional(readOnly = true)
    @Override
    public Page<ProductResponseDto> findUserBasedRecommendation(User user, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);

        List<Long> viewedProductIds = userProductViewRepository.findViewedProductIds(user.getUserId());

        if (!viewedProductIds.isEmpty()) {
            // 내가 본 상품을 똑같이 본 사람들이 그 밖에 무엇을 봤는지
            List<Long> coViewed = userProductViewRepository.findCoViewedProductIds(
                    user.getUserId(), viewedProductIds, (page + 1) * size);

            if (!coViewed.isEmpty()) {
                List<Long> pageIds = coViewed.stream().skip((long) page * size).limit(size).toList();
                Map<Long, Product> byId = productRepository.findAllById(pageIds).stream()
                        .collect(Collectors.toMap(Product::getId, Function.identity()));

                // 함께 본 횟수 순서를 유지한다
                List<ProductResponseDto> ordered = pageIds.stream()
                        .map(byId::get)
                        .filter(Objects::nonNull)
                        .map(ProductResponseDto::toDto)
                        .toList();

                return new PageImpl<>(ordered, pageable, coViewed.size());
            }
        }

        // 이력이 없거나 함께 본 사람이 없으면 베스트셀러로 채운다
        return findBestSellersOrRandom(pageable);
    }

    /**
     * 베스트셀러 조회, 없으면 랜덤 상품 반환 (페이징 적용)
     */
    private Page<ProductResponseDto> findBestSellersOrRandom(Pageable pageable) {
        Page<Product> bestSellingProducts = productRepository.findBestSellingProducts(pageable);

        if (!bestSellingProducts.isEmpty()) {
            return bestSellingProducts.map(ProductResponseDto::toDto);
        }

        Page<Product> randomProducts = productRepository.findRandomProducts(pageable);
        return randomProducts.map(ProductResponseDto::toDto);
    }
}
