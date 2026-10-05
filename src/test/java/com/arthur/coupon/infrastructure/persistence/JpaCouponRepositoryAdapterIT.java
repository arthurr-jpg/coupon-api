package com.arthur.coupon.infrastructure.persistence;

import com.arthur.coupon.domain.Coupon;
import com.arthur.coupon.domain.CouponStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.dao.OptimisticLockingFailureException;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@Import(JpaCouponRepositoryAdapter.class)
class JpaCouponRepositoryAdapterIT {

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");
    private static final Instant FUTURE = NOW.plus(10, ChronoUnit.DAYS);

    @Autowired
    private JpaCouponRepositoryAdapter adapter;

    @Autowired
    private TestEntityManager entityManager;

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }

    @Test
    void persistsAndReloadsAllFields() {
        Coupon coupon = Coupon.create("ABC-123", "Description", new BigDecimal("0.8"), FUTURE, true, NOW);
        adapter.save(coupon);
        flushAndClear();

        Coupon loaded = adapter.findById(coupon.getId()).orElseThrow();

        assertThat(loaded.getId()).isEqualTo(coupon.getId());
        assertThat(loaded.getCode()).isEqualTo("ABC123");
        assertThat(loaded.getDescription()).isEqualTo("Description");
        assertThat(loaded.getDiscountValue()).isEqualByComparingTo("0.8");
        assertThat(loaded.getExpirationDate()).isEqualTo(FUTURE);
        assertThat(loaded.getStatus()).isEqualTo(CouponStatus.ACTIVE);
        assertThat(loaded.isPublished()).isTrue();
        assertThat(loaded.isRedeemed()).isFalse();
    }

    @Test
    void softDeletedCouponIsKeptInTheDatabaseWithAllItsData() {
        Coupon coupon = Coupon.create("ABC123", "Description", new BigDecimal("0.8"), FUTURE, false, NOW);
        adapter.save(coupon);
        flushAndClear();

        Coupon loaded = adapter.findById(coupon.getId()).orElseThrow();
        loaded.delete(NOW.plusSeconds(30));
        adapter.save(loaded);
        flushAndClear();

        Coupon afterDelete = adapter.findById(coupon.getId()).orElseThrow();
        assertThat(afterDelete.getStatus()).isEqualTo(CouponStatus.DELETED);
        assertThat(afterDelete.getDeletedAt()).isEqualTo(NOW.plusSeconds(30));
        assertThat(afterDelete.getCode()).isEqualTo("ABC123");
        assertThat(afterDelete.getDescription()).isEqualTo("Description");
        assertThat(afterDelete.getDiscountValue()).isEqualByComparingTo("0.8");
        assertThat(afterDelete.getExpirationDate()).isEqualTo(FUTURE);
    }

    @Test
    void loadsCouponWhoseExpirationDateIsNowInThePast() {
        Instant longAgo = NOW.minus(365, ChronoUnit.DAYS);
        Coupon expired = Coupon.restore(UUID.randomUUID(), "OLD123", "Old coupon", new BigDecimal("2"),
                longAgo, CouponStatus.ACTIVE, true, false, null, null);
        adapter.save(expired);
        flushAndClear();

        assertThat(adapter.findById(expired.getId()).orElseThrow().getExpirationDate()).isEqualTo(longAgo);
    }

    @Test
    void secondConcurrentUpdateFailsInsteadOfOverwritingTheFirst() {
        Coupon coupon = Coupon.create("ABC123", "Description", new BigDecimal("0.8"), FUTURE, false, NOW);
        adapter.save(coupon);
        flushAndClear();

        Coupon first = adapter.findById(coupon.getId()).orElseThrow();
        Coupon second = adapter.findById(coupon.getId()).orElseThrow();

        first.delete(NOW.plusSeconds(10));
        adapter.save(first);
        flushAndClear();

        second.delete(NOW.plusSeconds(20));
        assertThatThrownBy(() -> {
            adapter.save(second);
            entityManager.flush();
        }).isInstanceOf(OptimisticLockingFailureException.class);

        entityManager.clear();
        assertThat(adapter.findById(coupon.getId()).orElseThrow().getDeletedAt())
                .isEqualTo(NOW.plusSeconds(10));
    }

    @Test
    void returnsEmptyWhenCouponDoesNotExist() {
        assertThat(adapter.findById(UUID.randomUUID())).isEmpty();
    }
}
