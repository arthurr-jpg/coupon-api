package com.arthur.coupon.domain;

import com.arthur.coupon.domain.exception.BusinessRuleException;
import com.arthur.coupon.domain.exception.CouponAlreadyDeletedException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CouponTest {

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");
    private static final Instant FUTURE = NOW.plus(10, ChronoUnit.DAYS);
    private static final BigDecimal DISCOUNT = new BigDecimal("0.8");

    private Coupon coupon(String code) {
        return Coupon.create(code, "Description", DISCOUNT, FUTURE, false, NOW);
    }

    @Test
    void createsActiveNotRedeemedCouponWithGeneratedId() {
        Coupon coupon = coupon("ABC123");

        assertThat(coupon.getId()).isNotNull();
        assertThat(coupon.getCode()).isEqualTo("ABC123");
        assertThat(coupon.getDescription()).isEqualTo("Description");
        assertThat(coupon.getDiscountValue()).isEqualByComparingTo("0.8");
        assertThat(coupon.getExpirationDate()).isEqualTo(FUTURE);
        assertThat(coupon.getStatus()).isEqualTo(CouponStatus.ACTIVE);
        assertThat(coupon.isPublished()).isFalse();
        assertThat(coupon.isRedeemed()).isFalse();
        assertThat(coupon.isDeleted()).isFalse();
        assertThat(coupon.getDeletedAt()).isNull();
    }

    @Test
    void canBeCreatedAlreadyPublished() {
        Coupon coupon = Coupon.create("ABC123", "Description", DISCOUNT, FUTURE, true, NOW);

        assertThat(coupon.isPublished()).isTrue();
    }

    @ParameterizedTest
    @CsvSource({"ABC-123,ABC123", "ab_c1.23,abc123", "!A!B!C!1!2!3!,ABC123", "AB C1 23,ABC123"})
    void removesSpecialCharactersFromCode(String input, String expected) {
        assertThat(coupon(input).getCode()).isEqualTo(expected);
    }

    @ParameterizedTest
    @ValueSource(strings = {"ABC12", "ABCDEFG", "A-B-C", "------", "ABC-12"})
    void rejectsCodeThatDoesNotHaveSixCharactersAfterSanitization(String input) {
        assertThatThrownBy(() -> coupon(input))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("exactly 6");
    }

    @Test
    void rejectsNullOrBlankCode() {
        assertThatThrownBy(() -> coupon(null)).isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(() -> coupon("   ")).isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void rejectsNullOrBlankDescription() {
        assertThatThrownBy(() -> Coupon.create("ABC123", null, DISCOUNT, FUTURE, false, NOW))
                .isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(() -> Coupon.create("ABC123", " ", DISCOUNT, FUTURE, false, NOW))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void rejectsDescriptionLongerThanTheLimit() {
        String tooLong = "x".repeat(Coupon.MAX_DESCRIPTION_LENGTH + 1);

        assertThatThrownBy(() -> Coupon.create("ABC123", tooLong, DISCOUNT, FUTURE, false, NOW))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("at most");
    }

    @Test
    void acceptsDescriptionWithExactlyTheMaximumLength() {
        String max = "x".repeat(Coupon.MAX_DESCRIPTION_LENGTH);

        assertThat(Coupon.create("ABC123", max, DISCOUNT, FUTURE, false, NOW).getDescription()).isEqualTo(max);
    }

    @ParameterizedTest
    @ValueSource(strings = {"0.49", "0", "-1"})
    void rejectsDiscountBelowMinimum(String value) {
        assertThatThrownBy(() -> Coupon.create("ABC123", "Description", new BigDecimal(value), FUTURE, false, NOW))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("at least 0.5");
    }

    @ParameterizedTest
    @ValueSource(strings = {"0.5", "0.8", "1000000"})
    void acceptsDiscountFromMinimumWithNoUpperLimit(String value) {
        Coupon coupon = Coupon.create("ABC123", "Description", new BigDecimal(value), FUTURE, false, NOW);

        assertThat(coupon.getDiscountValue()).isEqualByComparingTo(value);
    }

    @Test
    void rejectsNullDiscount() {
        assertThatThrownBy(() -> Coupon.create("ABC123", "Description", null, FUTURE, false, NOW))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void rejectsExpirationDateInThePast() {
        Instant past = NOW.minusSeconds(1);

        assertThatThrownBy(() -> Coupon.create("ABC123", "Description", DISCOUNT, past, false, NOW))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("past");
    }

    @Test
    void acceptsExpirationDateEqualToNow() {
        Coupon coupon = Coupon.create("ABC123", "Description", DISCOUNT, NOW, false, NOW);

        assertThat(coupon.getExpirationDate()).isEqualTo(NOW);
    }

    @Test
    void rejectsNullExpirationDate() {
        assertThatThrownBy(() -> Coupon.create("ABC123", "Description", DISCOUNT, null, false, NOW))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void deleteMarksCouponAsDeletedKeepingItsData() {
        Coupon coupon = coupon("ABC123");
        Instant deletionTime = NOW.plusSeconds(60);

        coupon.delete(deletionTime);

        assertThat(coupon.isDeleted()).isTrue();
        assertThat(coupon.getStatus()).isEqualTo(CouponStatus.DELETED);
        assertThat(coupon.getDeletedAt()).isEqualTo(deletionTime);
        assertThat(coupon.getCode()).isEqualTo("ABC123");
        assertThat(coupon.getDescription()).isEqualTo("Description");
        assertThat(coupon.getDiscountValue()).isEqualByComparingTo("0.8");
    }

    @Test
    void cannotDeleteAnAlreadyDeletedCoupon() {
        Coupon coupon = coupon("ABC123");
        coupon.delete(NOW);

        assertThatThrownBy(() -> coupon.delete(NOW.plusSeconds(1)))
                .isInstanceOf(CouponAlreadyDeletedException.class);
        assertThat(coupon.getDeletedAt()).isEqualTo(NOW);
    }

    @Test
    void restoredCouponDoesNotRevalidateExpirationDate() {
        Instant longAgo = NOW.minus(100, ChronoUnit.DAYS);

        Coupon restored = Coupon.restore(UUID.randomUUID(), "ABC123", "Description", DISCOUNT,
                longAgo, CouponStatus.ACTIVE, true, false, null, null);

        assertThat(restored.getExpirationDate()).isEqualTo(longAgo);
        assertThat(restored.isPublished()).isTrue();
    }

    @Test
    void restoredDeletedCouponCannotBeDeletedAgain() {
        Coupon restored = Coupon.restore(UUID.randomUUID(), "ABC123", "Description", DISCOUNT,
                FUTURE, CouponStatus.DELETED, false, false, NOW, null);

        assertThatThrownBy(() -> restored.delete(NOW.plusSeconds(1)))
                .isInstanceOf(CouponAlreadyDeletedException.class);
    }
}
