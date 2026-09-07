package com.Soo_Shinsa.product.service;

import com.Soo_Shinsa.product.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * 자동완성 가드 검증. 짧은 키워드로 LIKE '%a%' 전체 스캔이 나가면 안 된다.
 */
@ExtendWith(MockitoExtension.class)
class ProductAutocompleteTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductServiceImpl productService;

    @Test
    void 두글자_미만이면_조회하지_않는다() {
        for (String keyword : new String[]{null, "", " ", "ا", "가", "  a  "}) {
            assertTrue(productService.autocomplete(keyword, 8).isEmpty(), "keyword=" + keyword);
        }
        verify(productRepository, never()).findNameSuggestions(any(), any());
    }

    @Test
    void 키워드는_트림해서_넘긴다() {
        lenient().when(productRepository.findNameSuggestions(eq("셔츠"), any())).thenReturn(List.of("옥스포드 셔츠"));
        assertEquals(List.of("옥스포드 셔츠"), productService.autocomplete("  셔츠  ", 8));
    }

    @Test
    void limit은_1에서_20_사이로_묶인다() {
        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        lenient().when(productRepository.findNameSuggestions(any(), any())).thenReturn(List.of());

        productService.autocomplete("셔츠", 9999);
        productService.autocomplete("셔츠", 0);
        productService.autocomplete("셔츠", -5);

        verify(productRepository, org.mockito.Mockito.times(3)).findNameSuggestions(any(), captor.capture());
        assertEquals(20, captor.getAllValues().get(0).getPageSize());
        assertEquals(1, captor.getAllValues().get(1).getPageSize());
        assertEquals(1, captor.getAllValues().get(2).getPageSize());
    }
}
