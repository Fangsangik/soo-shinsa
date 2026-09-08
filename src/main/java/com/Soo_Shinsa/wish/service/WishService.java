package com.Soo_Shinsa.wish.service;

import com.Soo_Shinsa.global.exception.DuplicatedException;
import com.Soo_Shinsa.global.exception.ErrorCode;
import com.Soo_Shinsa.global.exception.NotFoundException;
import com.Soo_Shinsa.product.model.Product;
import com.Soo_Shinsa.product.repository.ProductRepository;
import com.Soo_Shinsa.user.model.User;
import com.Soo_Shinsa.wish.dto.WishResponseDto;
import com.Soo_Shinsa.wish.model.Wish;
import com.Soo_Shinsa.wish.repository.WishRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class WishService {

    private final WishRepository wishRepository;
    private final ProductRepository productRepository;

    @Transactional
    public WishResponseDto add(User user, Long productId) {
        if (wishRepository.existsByUserUserIdAndProductId(user.getUserId(), productId)) {
            throw new DuplicatedException(ErrorCode.ALREADY_WISHED);
        }
        Product product = productRepository.findByIdOrElseThrow(productId);
        Wish wish = wishRepository.save(new Wish(user, product));
        return WishResponseDto.from(wish);
    }

    @Transactional
    public void remove(User user, Long productId) {
        Wish wish = wishRepository.findByUserUserIdAndProductId(user.getUserId(), productId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.NOT_FOUND_WISH));
        wishRepository.delete(wish);
    }

    @Transactional(readOnly = true)
    public List<WishResponseDto> findMine(User user) {
        return wishRepository.findAllByUserUserIdOrderByIdDesc(user.getUserId()).stream()
                .map(WishResponseDto::from)
                .toList();
    }
}
