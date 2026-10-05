package com.arthur.coupon.domain.exception;

import java.util.UUID;

public class CouponAlreadyDeletedException extends RuntimeException {

    public CouponAlreadyDeletedException(UUID id) {
        super("Coupon already deleted: " + id);
    }
}
