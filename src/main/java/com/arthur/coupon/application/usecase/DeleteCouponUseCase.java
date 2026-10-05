package com.arthur.coupon.application.usecase;

import com.arthur.coupon.application.exception.CouponNotFoundException;
import com.arthur.coupon.application.port.CouponRepository;
import com.arthur.coupon.application.port.CurrentTime;
import com.arthur.coupon.domain.Coupon;

import java.util.UUID;

public class DeleteCouponUseCase {

    private final CouponRepository repository;
    private final CurrentTime currentTime;

    public DeleteCouponUseCase(CouponRepository repository, CurrentTime currentTime) {
        this.repository = repository;
        this.currentTime = currentTime;
    }

    public void execute(UUID id) {
        Coupon coupon = repository.findById(id).orElseThrow(() -> new CouponNotFoundException(id));
        coupon.delete(currentTime.now());
        repository.save(coupon);
    }
}
