package com.arthur.coupon.domain;

import com.arthur.coupon.domain.exception.BusinessRuleException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CouponCodeTest {

    @ParameterizedTest
    @CsvSource({"ABC-123,ABC123", "ab_c1.23,abc123", "!A!B!C!1!2!3!,ABC123"})
    void sanitizingRemovesSpecialCharactersPreservingCase(String raw, String expected) {
        assertThat(CouponCode.sanitizing(raw).value()).isEqualTo(expected);
    }

    @ParameterizedTest
    @ValueSource(strings = {"ABC12", "ABCDEFG", "------", "AB-C1"})
    void sanitizingRejectsWhenResultIsNotSixCharacters(String raw) {
        assertThatThrownBy(() -> CouponCode.sanitizing(raw))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("exactly 6");
    }

    @Test
    void sanitizingRejectsNullAndBlank() {
        assertThatThrownBy(() -> CouponCode.sanitizing(null)).isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(() -> CouponCode.sanitizing("  ")).isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void constructorNeverAllowsSpecialCharacters() {
        assertThatThrownBy(() -> new CouponCode("ABC-12")).isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void constructorAcceptsExactlySixAlphanumericCharacters() {
        assertThat(new CouponCode("aB3dE9").value()).isEqualTo("aB3dE9");
    }
}
