package com.Soo_Shinsa.product.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class FindProductOptionRequestDto {

    private String size;
    private String color;

    /**
     * 조회 파라미터는 optionSize/color 만 받는다.
     *
     * 클래스에 @Setter 를 붙였더니 페이징 파라미터 ?size=10 이 setSize("10") 으로
     * 바인딩되어 size='10' 인 옵션을 찾았다(항상 0건). 그래서 setSize 는 만들지 않는다.
     */
    public void setOptionSize(String optionSize) {
        this.size = optionSize;
    }

    public void setColor(String color) {
        this.color = color;
    }


    public FindProductOptionRequestDto(String size, String color) {
        this.size = size;
        this.color = color;
    }
}
