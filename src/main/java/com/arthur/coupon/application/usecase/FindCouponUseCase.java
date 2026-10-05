package com.arthur.coupon.application.usecase;

import com.arthur.coupon.application.exception.CouponNotFoundException;
import com.arthur.coupon.application.port.CouponRepository;
import com.arthur.coupon.domain.Coupon;

import java.util.UUID;

public class FindCouponUseCase {

    private final CouponRepository repository;

    public FindCouponUseCase(CouponRepository repository) {
        this.repository = repository;
    }

    public Coupon execute(UUID id) {
        return repository.findById(id).orElseThrow(() -> new CouponNotFoundException(id));
    }
}
