package com.arthur.coupon.application.exception;

import java.util.UUID;

public class CouponNotFoundException extends RuntimeException {

    public CouponNotFoundException(UUID id) {
        super("Coupon not found: " + id);
    }
}
