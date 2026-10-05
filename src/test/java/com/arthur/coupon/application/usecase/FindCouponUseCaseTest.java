package com.arthur.coupon.application.usecase;

import com.arthur.coupon.application.InMemoryCouponRepository;
import com.arthur.coupon.application.exception.CouponNotFoundException;
import com.arthur.coupon.domain.Coupon;
import com.arthur.coupon.domain.CouponStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FindCouponUseCaseTest {

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

    private final InMemoryCouponRepository repository = new InMemoryCouponRepository();
    private final FindCouponUseCase useCase = new FindCouponUseCase(repository);

    private Coupon storedCoupon() {
        Coupon coupon = Coupon.create("ABC123", "Description", new BigDecimal("0.8"),
                NOW.plus(5, ChronoUnit.DAYS), false, NOW);
        repository.save(coupon);
        return coupon;
    }

    @Test
    void returnsExistingCoupon() {
        Coupon coupon = storedCoupon();

        assertThat(useCase.execute(coupon.getId()).getCode()).isEqualTo("ABC123");
    }

    @Test
    void stillReturnsSoftDeletedCouponWithDeletedStatus() {
        Coupon coupon = storedCoupon();
        coupon.delete(NOW);
        repository.save(coupon);

        assertThat(useCase.execute(coupon.getId()).getStatus()).isEqualTo(CouponStatus.DELETED);
    }

    @Test
    void failsWhenCouponDoesNotExist() {
        assertThatThrownBy(() -> useCase.execute(UUID.randomUUID()))
                .isInstanceOf(CouponNotFoundException.class);
    }
}
