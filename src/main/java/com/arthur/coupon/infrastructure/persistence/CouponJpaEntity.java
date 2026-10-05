package com.arthur.coupon.infrastructure.persistence;

import com.arthur.coupon.domain.Coupon;
import com.arthur.coupon.domain.CouponStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Modelo de persistencia. Nao contem regra de negocio: so mapeia de/para o dominio.
 */
@Entity
@Table(name = "coupons")
public class CouponJpaEntity {

    @Id
    private UUID id;

    @Column(nullable = false, length = 6)
    private String code;

    @Column(nullable = false, length = 2000)
    private String description;

    @Column(name = "discount_value", nullable = false, precision = 38, scale = 4)
    private BigDecimal discountValue;

    @Column(name = "expiration_date", nullable = false)
    private Instant expirationDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CouponStatus status;

    @Column(nullable = false)
    private boolean published;

    @Column(nullable = false)
    private boolean redeemed;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @Version
    private Long version;

    protected CouponJpaEntity() {
        // exigido pelo JPA
    }

    static CouponJpaEntity fromDomain(Coupon coupon) {
        CouponJpaEntity entity = new CouponJpaEntity();
        entity.id = coupon.getId();
        entity.code = coupon.getCode();
        entity.description = coupon.getDescription();
        entity.discountValue = coupon.getDiscountValue();
        entity.expirationDate = coupon.getExpirationDate();
        entity.status = coupon.getStatus();
        entity.published = coupon.isPublished();
        entity.redeemed = coupon.isRedeemed();
        entity.deletedAt = coupon.getDeletedAt();
        entity.version = coupon.getVersion();
        return entity;
    }

    Coupon toDomain() {
        return Coupon.restore(id, code, description, discountValue, expirationDate,
                status, published, redeemed, deletedAt, version);
    }
}
