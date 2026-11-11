package com.Soo_Shinsa.wish.service;

import com.Soo_Shinsa.constant.Role;
import com.Soo_Shinsa.exception.DuplicatedException;
import com.Soo_Shinsa.exception.NotFoundException;
import com.Soo_Shinsa.product.model.Product;
import com.Soo_Shinsa.product.repository.ProductRepository;
import com.Soo_Shinsa.user.model.User;
import com.Soo_Shinsa.user.repository.UserRepository;
import com.Soo_Shinsa.wish.dto.WishResponseDto;
import com.Soo_Shinsa.wish.model.Wish;
import com.Soo_Shinsa.wish.repository.WishRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static com.Soo_Shinsa.support.Fixtures.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WishServiceTest {
    @Mock WishRepository wishRepository;
    @Mock ProductRepository productRepository;
    @Mock UserRepository userRepository;
    @InjectMocks WishService service;

    User me = user(1L, Role.CUSTOMER);
    Product product = product(10L, brand(100L, user(3L, Role.VENDOR)), 10_000);

    @Test
    void 같은_상품은_두_번_찜할_수_없고_없는_찜은_해제할_수_없다() {
        when(wishRepository.existsByUserUserIdAndProductId(1L, 10L)).thenReturn(true);
        assertThatThrownBy(() -> service.add(me, 10L)).isInstanceOf(DuplicatedException.class);
        when(wishRepository.findByUserUserIdAndProductId(1L, 10L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.remove(me, 10L)).isInstanceOf(NotFoundException.class);
        verify(wishRepository, never()).save(any());
    }

    @Test
    void 찜_추가는_영속_사용자로_저장하고_상품_정보를_돌려준다() {
        when(wishRepository.existsByUserUserIdAndProductId(1L, 10L)).thenReturn(false);
        when(productRepository.findByIdOrElseThrow(10L)).thenReturn(product);
        when(userRepository.findByIdOrElseThrow(1L)).thenReturn(me);
        when(wishRepository.save(any(Wish.class))).thenAnswer(i -> withId(i.getArgument(0), "id", 5L));

        WishResponseDto dto = service.add(me, 10L);

        assertThat(dto.id()).isEqualTo(5L);
        assertThat(dto.productId()).isEqualTo(10L);
        assertThat(dto.brandName()).isEqualTo("brand100");
        assertThat(dto.price()).isEqualByComparingTo("10000");
    }

    @Test
    void 내_찜_목록() {
        when(wishRepository.findAllByUserUserIdOrderByIdDesc(1L)).thenReturn(List.of(withId(new Wish(me, product), "id", 1L)));
        assertThat(service.findMine(me)).extracting(WishResponseDto::productName).containsExactly("product10");
    }
}
