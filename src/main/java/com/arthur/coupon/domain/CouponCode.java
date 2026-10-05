package com.arthur.coupon.domain;

import com.arthur.coupon.domain.exception.BusinessRuleException;

import java.util.regex.Pattern;

/**
 * Value object: um codigo de cupom so existe se tiver exatamente 6 caracteres alfanumericos.
 */
public record CouponCode(String value) {

    public static final int LENGTH = 6;
    private static final Pattern SPECIAL_CHARACTERS = Pattern.compile("[^A-Za-z0-9]");

    public CouponCode {
        if (value == null || value.length() != LENGTH || SPECIAL_CHARACTERS.matcher(value).find()) {
            throw new BusinessRuleException("Coupon code must have exactly " + LENGTH
                    + " alphanumeric characters after removing special characters");
        }
    }

    /**
     * Aceita caracteres especiais na entrada, remove-os e valida o tamanho resultante.
     */
    public static CouponCode sanitizing(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new BusinessRuleException("Coupon code is required");
        }
        return new CouponCode(SPECIAL_CHARACTERS.matcher(raw).replaceAll(""));
    }
}
