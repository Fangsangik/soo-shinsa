package com.Soo_Shinsa.utils;

import com.Soo_Shinsa.exception.NoAuthorizedException;
import com.Soo_Shinsa.order.model.OrderItem;
import com.Soo_Shinsa.order.model.Orders;
import com.Soo_Shinsa.user.model.User;

import static com.Soo_Shinsa.exception.ErrorCode.NO_AUTHORITY;
import static com.Soo_Shinsa.exception.ErrorCode.NO_CONNECT;

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

    /** ADMIN 은 모든 브랜드, VENDOR 는 자기 브랜드만 */
    public static void validateBrandOwner(User user, com.Soo_Shinsa.brand.model.Brand brand) {
        validateAdminOrVendorAccess(user);
        if (!user.isAdmin() && (brand.getUser() == null || !brand.getUser().getUserId().equals(user.getUserId()))) {
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
