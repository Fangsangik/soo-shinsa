package com.Soo_Shinsa.global.utils;

import com.Soo_Shinsa.global.exception.NoAuthorizedException;
import com.Soo_Shinsa.order.model.OrderItem;
import com.Soo_Shinsa.order.model.Orders;
import com.Soo_Shinsa.user.model.User;

import static com.Soo_Shinsa.global.exception.ErrorCode.NO_AUTHORITY;
import static com.Soo_Shinsa.global.exception.ErrorCode.NO_CONNECT;

public class EntityValidator {

    public static void validateUserOwnership(Long userId, Long ownerId) {
        if (!userId.equals(ownerId)) {
            throw new NoAuthorizedException(NO_AUTHORITY);
        }
    }

    public static void validateAdminAccess(User user) {
        if (!user.isAdmin()) {
            throw new NoAuthorizedException(NO_AUTHORITY);
        }
    }

    public static void validateAdminOrVendorAccess(User user) {
        if (!user.isAdminOrVendor()) {
            throw new NoAuthorizedException(NO_AUTHORITY);
        }
    }

    public static void validateAndOrders(Orders orders,Long userId) {
        if (!orders.getUser().getUserId().equals(userId)) {
            throw new NoAuthorizedException(NO_CONNECT);
        }
    }

    public static void validateAndOrderItem(OrderItem orderItem,Long userId) {
        if (!orderItem.getOrder().getUser().getUserId().equals(userId)) {
            throw new NoAuthorizedException(NO_CONNECT);
        }
    }
}
