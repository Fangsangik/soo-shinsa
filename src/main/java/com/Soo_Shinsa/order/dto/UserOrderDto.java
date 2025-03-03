package com.Soo_Shinsa.order.dto;


import com.Soo_Shinsa.order.model.Orders;
import com.Soo_Shinsa.user.model.User;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class UserOrderDto {
    private User user;
    private Orders order;

    public UserOrderDto(User user, Orders order) {
        this.user = user;
        this.order = order;
    }
}