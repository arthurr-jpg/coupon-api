package com.arthur.coupon.domain;

import com.arthur.coupon.domain.exception.BusinessRuleException;
import com.arthur.coupon.domain.exception.CouponAlreadyDeletedException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Entidade de dominio (Java puro). Concentra as regras de negocio do cupom:
 * so se cria via {@link #create} e so se remove via {@link #delete}.
 */
public class Coupon {

    public static final int MAX_DESCRIPTION_LENGTH = 2000;

    private final UUID id;
    private final CouponCode code;
    private final String description;
    private final DiscountValue discountValue;
    private final Instant expirationDate;
    private final boolean published;
    private final boolean redeemed;
    private CouponStatus status;
    private Instant deletedAt;
    private final Long version;

    private Coupon(UUID id,
                   CouponCode code,
                   String description,
                   DiscountValue discountValue,
                   Instant expirationDate,
                   CouponStatus status,
                   boolean published,
                   boolean redeemed,
                   Instant deletedAt,
                   Long version) {
        this.id = id;
        this.code = code;
        this.description = description;
        this.discountValue = discountValue;
        this.expirationDate = expirationDate;
        this.status = status;
        this.published = published;
        this.redeemed = redeemed;
        this.deletedAt = deletedAt;
        this.version = version;
    }

    /**
     * Cadastro de um cupom novo: aplica todas as regras de criacao.
     */
    public static Coupon create(String code,
                                String description,
                                BigDecimal discountValue,
                                Instant expirationDate,
                                boolean published,
                                Instant now) {
        return new Coupon(
                UUID.randomUUID(),
                CouponCode.sanitizing(code),
                requireDescription(description),
                new DiscountValue(discountValue),
                requireNotInPast(expirationDate, now),
                CouponStatus.ACTIVE,
                published,
                false,
                null,
                null);
    }

    /**
     * Reconstitui um cupom ja existente (vindo da persistencia). Nao revalida a data de expiracao,
     * pois um cupom valido na criacao pode expirar depois.
     */
    public static Coupon restore(UUID id,
                                 String code,
                                 String description,
                                 BigDecimal discountValue,
                                 Instant expirationDate,
                                 CouponStatus status,
                                 boolean published,
                                 boolean redeemed,
                                 Instant deletedAt,
                                 Long version) {
        return new Coupon(
                Objects.requireNonNull(id),
                new CouponCode(code),
                description,
                new DiscountValue(discountValue),
                Objects.requireNonNull(expirationDate),
                Objects.requireNonNull(status),
                published,
                redeemed,
                deletedAt,
                version);
    }

    /**
     * Soft delete: mantem todos os dados do cadastro e apenas marca o cupom como deletado.
     */
    public void delete(Instant now) {
        if (isDeleted()) {
            throw new CouponAlreadyDeletedException(id);
        }
        this.status = CouponStatus.DELETED;
        this.deletedAt = now;
    }

    public boolean isDeleted() {
        return status == CouponStatus.DELETED;
    }

    private static String requireDescription(String description) {
        if (description == null || description.isBlank()) {
            throw new BusinessRuleException("Coupon description is required");
        }
        if (description.length() > MAX_DESCRIPTION_LENGTH) {
            throw new BusinessRuleException(
                    "Coupon description must have at most " + MAX_DESCRIPTION_LENGTH + " characters");
        }
        return description;
    }

    private static Instant requireNotInPast(Instant expirationDate, Instant now) {
        if (expirationDate == null) {
            throw new BusinessRuleException("Coupon expiration date is required");
        }
        if (expirationDate.isBefore(now)) {
            throw new BusinessRuleException("Coupon expiration date cannot be in the past");
        }
        return expirationDate;
    }

    public UUID getId() {
        return id;
    }

    public String getCode() {
        return code.value();
    }

    public String getDescription() {
        return description;
    }

    public BigDecimal getDiscountValue() {
        return discountValue.value();
    }

    public Instant getExpirationDate() {
        return expirationDate;
    }

    public CouponStatus getStatus() {
        return status;
    }

    public boolean isPublished() {
        return published;
    }

    public boolean isRedeemed() {
        return redeemed;
    }

    public Instant getDeletedAt() {
        return deletedAt;
    }

    /**
     * Versao lida da persistencia, usada para deteccao de alteracao concorrente (lock otimista).
     * O dominio apenas a carrega; nunca a interpreta. E nula para cupons ainda nao salvos.
     */
    public Long getVersion() {
        return version;
    }
}
