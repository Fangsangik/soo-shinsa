package com.Soo_Shinsa.product.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

@Getter
@Setter // @ModelAttribute 쿼리파라미터 바인딩용
@NoArgsConstructor
public class FindProductOptionRequestDto {

    private String size;
    private String color;

    /**
     * 조회 파라미터 이름은 optionSize 를 쓴다.
     * size 는 페이징 크기(@RequestParam int size)와 이름이 겹쳐서
     * ?size=M 으로 부르면 int 변환에 실패해 400 이 났다.
     */
    public void setOptionSize(String optionSize) {
        this.size = optionSize;
    }


    public FindProductOptionRequestDto(String size, String color) {
        this.size = size;
        this.color = color;
    }
}
