package com.arthur.coupon.application.usecase;

import com.arthur.coupon.application.InMemoryCouponRepository;
import com.arthur.coupon.domain.Coupon;
import com.arthur.coupon.domain.CouponStatus;
import com.arthur.coupon.domain.exception.BusinessRuleException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CreateCouponUseCaseTest {

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");
    private static final Instant FUTURE = NOW.plus(10, ChronoUnit.DAYS);

    private InMemoryCouponRepository repository;
    private CreateCouponUseCase useCase;

    @BeforeEach
    void setUp() {
        repository = new InMemoryCouponRepository();
        useCase = new CreateCouponUseCase(repository, () -> NOW);
    }

    private CreateCouponCommand command(String code, String discount, Instant expiration, boolean published) {
        return new CreateCouponCommand(code, "Description", new BigDecimal(discount), expiration, published);
    }

    @Test
    void createsCouponAndStoresItWithSanitizedCode() {
        Coupon created = useCase.execute(command("ABC-123", "0.8", FUTURE, false));

        assertThat(created.getCode()).isEqualTo("ABC123");
        Coupon stored = repository.findById(created.getId()).orElseThrow();
        assertThat(stored.getCode()).isEqualTo("ABC123");
        assertThat(stored.getStatus()).isEqualTo(CouponStatus.ACTIVE);
        assertThat(stored.isRedeemed()).isFalse();
        assertThat(stored.isPublished()).isFalse();
    }

    @Test
    void canCreateCouponAlreadyPublished() {
        Coupon created = useCase.execute(command("ABC123", "0.8", FUTURE, true));

        assertThat(repository.findById(created.getId()).orElseThrow().isPublished()).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"ABC12", "ABCDEFG", "------", "", "  "})
    void rejectsInvalidCodeAndStoresNothing(String code) {
        assertThatThrownBy(() -> useCase.execute(command(code, "0.8", FUTURE, false)))
                .isInstanceOf(BusinessRuleException.class);
        assertThat(repository.count()).isZero();
    }

    @ParameterizedTest
    @ValueSource(strings = {"0.49", "0", "-3"})
    void rejectsDiscountBelowMinimumAndStoresNothing(String discount) {
        assertThatThrownBy(() -> useCase.execute(command("ABC123", discount, FUTURE, false)))
                .isInstanceOf(BusinessRuleException.class);
        assertThat(repository.count()).isZero();
    }

    @Test
    void rejectsExpirationInThePastAccordingToCurrentTimeAndStoresNothing() {
        assertThatThrownBy(() -> useCase.execute(command("ABC123", "0.8", NOW.minusSeconds(1), false)))
                .isInstanceOf(BusinessRuleException.class);
        assertThat(repository.count()).isZero();
    }

    @Test
    void acceptsMinimumDiscountAndVeryLargeDiscount() {
        useCase.execute(command("MIN123", "0.5", FUTURE, false));
        useCase.execute(command("BIG123", "100000000", FUTURE, false));

        assertThat(repository.count()).isEqualTo(2);
    }
}
