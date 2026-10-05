package com.arthur.coupon.domain;

import com.arthur.coupon.domain.exception.BusinessRuleException;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Value object: valor de desconto minimo de 0.5, sem maximo de negocio.
 * Existe apenas o limite tecnico do armazenamento (34 digitos inteiros, 4 casas decimais).
 */
public record DiscountValue(BigDecimal value) {

    public static final BigDecimal MINIMUM = new BigDecimal("0.5");
    public static final int SCALE = 4;
    public static final int MAX_INTEGER_DIGITS = 34;

    public DiscountValue {
        if (value == null) {
            throw new BusinessRuleException("Coupon discount value is required");
        }
        if (value.compareTo(MINIMUM) < 0) {
            throw new BusinessRuleException("Coupon discount value must be at least " + MINIMUM);
        }
        if (value.precision() - value.scale() > MAX_INTEGER_DIGITS) {
            throw new BusinessRuleException("Coupon discount value exceeds the supported range");
        }
        value = value.setScale(SCALE, RoundingMode.HALF_UP);
    }
}
