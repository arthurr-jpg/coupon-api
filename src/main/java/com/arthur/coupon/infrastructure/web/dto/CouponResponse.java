package com.arthur.coupon.infrastructure.web.dto;

import com.arthur.coupon.domain.Coupon;
import com.arthur.coupon.domain.CouponStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Schema(description = "Cupom")
public record CouponResponse(

        @Schema(description = "Identificador unico", example = "cef9d1e3-aae5-4ab6-a297-358c6032b1e7")
        UUID id,

        @Schema(description = "Codigo ja sanitizado (6 caracteres alfanumericos)", example = "ABC123")
        String code,

        @Schema(description = "Descricao do cupom", example = "10% de desconto na primeira compra")
        String description,

        @Schema(description = "Valor do desconto", example = "0.8")
        BigDecimal discountValue,

        @Schema(description = "Data de expiracao (UTC)", example = "2030-11-04T17:14:45.180Z")
        Instant expirationDate,

        @Schema(description = "Situacao do cupom. DELETED indica soft delete.",
                example = "ACTIVE", allowableValues = {"ACTIVE", "INACTIVE", "DELETED"})
        CouponStatus status,

        @Schema(description = "Se o cupom esta publicado", example = "false")
        boolean published,

        @Schema(description = "Se o cupom ja foi resgatado", example = "false")
        boolean redeemed) {

    public static CouponResponse from(Coupon coupon) {
        return new CouponResponse(
                coupon.getId(),
                coupon.getCode(),
                coupon.getDescription(),
                normalize(coupon.getDiscountValue()),
                coupon.getExpirationDate(),
                coupon.getStatus(),
                coupon.isPublished(),
                coupon.isRedeemed());
    }

    private static BigDecimal normalize(BigDecimal value) {
        BigDecimal stripped = value.stripTrailingZeros();
        return stripped.scale() < 0 ? stripped.setScale(0) : stripped;
    }
}
