package com.arthur.coupon.application.usecase;

import com.arthur.coupon.application.InMemoryCouponRepository;
import com.arthur.coupon.application.exception.CouponNotFoundException;
import com.arthur.coupon.domain.Coupon;
import com.arthur.coupon.domain.CouponStatus;
import com.arthur.coupon.domain.exception.CouponAlreadyDeletedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DeleteCouponUseCaseTest {

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");
    private static final Instant FUTURE = NOW.plus(10, ChronoUnit.DAYS);

    private InMemoryCouponRepository repository;
    private DeleteCouponUseCase useCase;
    private UUID couponId;

    @BeforeEach
    void setUp() {
        repository = new InMemoryCouponRepository();
        useCase = new DeleteCouponUseCase(repository, () -> NOW);

        Coupon coupon = Coupon.create("ABC123", "Description", new BigDecimal("0.8"), FUTURE, true, NOW);
        repository.save(coupon);
        couponId = coupon.getId();
    }

    @Test
    void softDeletesKeepingEverythingReceivedAtRegistration() {
        useCase.execute(couponId);

        assertThat(repository.count()).isEqualTo(1);
        Coupon stored = repository.findById(couponId).orElseThrow();
        assertThat(stored.getStatus()).isEqualTo(CouponStatus.DELETED);
        assertThat(stored.getDeletedAt()).isEqualTo(NOW);
        assertThat(stored.getCode()).isEqualTo("ABC123");
        assertThat(stored.getDescription()).isEqualTo("Description");
        assertThat(stored.getDiscountValue()).isEqualByComparingTo("0.8");
        assertThat(stored.getExpirationDate()).isEqualTo(FUTURE);
        assertThat(stored.isPublished()).isTrue();
    }

    @Test
    void cannotDeleteTheSameCouponTwiceAndKeepsTheFirstDeletionTime() {
        useCase.execute(couponId);
        DeleteCouponUseCase later = new DeleteCouponUseCase(repository, () -> NOW.plus(1, ChronoUnit.HOURS));

        assertThatThrownBy(() -> later.execute(couponId))
                .isInstanceOf(CouponAlreadyDeletedException.class);

        Coupon stored = repository.findById(couponId).orElseThrow();
        assertThat(stored.getStatus()).isEqualTo(CouponStatus.DELETED);
        assertThat(stored.getDeletedAt()).isEqualTo(NOW);
    }

    @Test
    void failsWhenCouponDoesNotExist() {
        assertThatThrownBy(() -> useCase.execute(UUID.randomUUID()))
                .isInstanceOf(CouponNotFoundException.class);
    }
}
