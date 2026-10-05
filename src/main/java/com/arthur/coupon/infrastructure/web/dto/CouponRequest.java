package com.arthur.coupon.infrastructure.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Instant;

@Schema(description = "Dados para criacao de um cupom")
public record CouponRequest(

        @Schema(description = "Codigo do cupom. Caracteres especiais sao removidos; "
                + "o resultado deve ter exatamente 6 caracteres alfanumericos.",
                example = "ABC-123", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "code is required")
        String code,

        @Schema(description = "Descricao do cupom",
                example = "10% de desconto na primeira compra", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "description is required")
        String description,

        @Schema(description = "Valor do desconto (minimo 0.5, sem maximo)",
                example = "0.8", minimum = "0.5", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "discountValue is required")
        BigDecimal discountValue,

        @Schema(description = "Data de expiracao (ISO-8601, UTC). Nao pode estar no passado.",
                example = "2030-11-04T17:14:45.180Z", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "expirationDate is required")
        Instant expirationDate,

        @Schema(description = "Indica se o cupom ja nasce publicado", example = "false", defaultValue = "false")
        boolean published) {
}
