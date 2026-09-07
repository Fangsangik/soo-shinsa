package com.Soo_Shinsa.product.service;

import com.Soo_Shinsa.product.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * 자동완성 가드 검증. 짧은 키워드로 전체 스캔이 나가면 안 되고,
 * BOOLEAN MODE 연산자가 그대로 쿼리에 실리면 안 된다.
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
        verify(productRepository, never()).findNameSuggestions(any(), any(), anyInt());
    }

    @Test
    void 키워드는_트림해서_넘긴다() {
        lenient().when(productRepository.findNameSuggestions(eq("\"셔츠\""), eq("셔츠"), anyInt()))
                .thenReturn(List.of("옥스포드 셔츠"));
        assertEquals(List.of("옥스포드 셔츠"), productService.autocomplete("  셔츠  ", 8));
    }

    @Test
    void 따옴표로_감싸_BOOLEAN_MODE_연산자를_무력화한다() {
        ArgumentCaptor<String> phrase = ArgumentCaptor.forClass(String.class);
        lenient().when(productRepository.findNameSuggestions(any(), any(), anyInt())).thenReturn(List.of());

        productService.autocomplete("셔츠\" OR 1=1", 8);

        verify(productRepository).findNameSuggestions(phrase.capture(), any(), anyInt());
        String sent = phrase.getValue();
        assertTrue(sent.startsWith("\"") && sent.endsWith("\""), sent);
        assertEquals(2, sent.chars().filter(ch -> ch == '"').count(), "따옴표가 남아 구문이 깨지면 안 된다: " + sent);
    }

    @Test
    void limit은_1에서_20_사이로_묶인다() {
        ArgumentCaptor<Integer> limit = ArgumentCaptor.forClass(Integer.class);
        lenient().when(productRepository.findNameSuggestions(any(), any(), anyInt())).thenReturn(List.of());

        productService.autocomplete("셔츠", 9999);
        productService.autocomplete("셔츠", 0);
        productService.autocomplete("셔츠", -5);

        verify(productRepository, times(3)).findNameSuggestions(any(), any(), limit.capture());
        assertEquals(20, limit.getAllValues().get(0));
        assertEquals(1, limit.getAllValues().get(1));
        assertEquals(1, limit.getAllValues().get(2));
    }
}
