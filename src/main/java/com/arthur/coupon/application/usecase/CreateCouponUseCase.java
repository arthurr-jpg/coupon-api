package com.arthur.coupon.application.usecase;

import com.arthur.coupon.application.port.CouponRepository;
import com.arthur.coupon.application.port.CurrentTime;
import com.arthur.coupon.domain.Coupon;

public class CreateCouponUseCase {

    private final CouponRepository repository;
    private final CurrentTime currentTime;

    public CreateCouponUseCase(CouponRepository repository, CurrentTime currentTime) {
        this.repository = repository;
        this.currentTime = currentTime;
    }

    public Coupon execute(CreateCouponCommand command) {
        Coupon coupon = Coupon.create(
                command.code(),
                command.description(),
                command.discountValue(),
                command.expirationDate(),
                command.published(),
                currentTime.now());
        return repository.save(coupon);
    }
}
