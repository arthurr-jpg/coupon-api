package com.arthur.coupon.domain;

import com.arthur.coupon.domain.exception.BusinessRuleException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DiscountValueTest {

    @ParameterizedTest
    @ValueSource(strings = {"0.49", "0.4999", "0", "-5"})
    void rejectsValuesBelowMinimum(String value) {
        assertThatThrownBy(() -> new DiscountValue(new BigDecimal(value)))
                .isInstanceOf(BusinessRuleException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"0.5", "0.51", "1", "999999999999"})
    void acceptsMinimumAndAboveWithNoMaximum(String value) {
        assertThat(new DiscountValue(new BigDecimal(value)).value()).isEqualByComparingTo(value);
    }

    @Test
    void rejectsNull() {
        assertThatThrownBy(() -> new DiscountValue(null)).isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void rejectsValuesBeyondTheSupportedStorageRange() {
        BigDecimal tooBig = new BigDecimal("1" + "0".repeat(40));

        assertThatThrownBy(() -> new DiscountValue(tooBig))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("supported range");
    }

    @Test
    void acceptsTheLargestSupportedValue() {
        BigDecimal largest = new BigDecimal("9".repeat(DiscountValue.MAX_INTEGER_DIGITS));

        assertThat(new DiscountValue(largest).value()).isEqualByComparingTo(largest);
    }

    @Test
    void normalizesToFourDecimalPlacesAfterValidating() {
        assertThat(new DiscountValue(new BigDecimal("0.50004")).value()).isEqualByComparingTo("0.5");
        assertThat(new DiscountValue(new BigDecimal("0.50004")).value().scale()).isEqualTo(4);
        assertThatThrownBy(() -> new DiscountValue(new BigDecimal("0.49999")))
                .isInstanceOf(BusinessRuleException.class);
    }
}
