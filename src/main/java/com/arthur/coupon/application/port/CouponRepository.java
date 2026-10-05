package com.arthur.coupon.application.port;

import com.arthur.coupon.domain.Coupon;

import java.util.Optional;
import java.util.UUID;

/**
 * Porta de saida: a aplicacao declara o que precisa; a infraestrutura decide como entregar.
 */
public interface CouponRepository {

    Coupon save(Coupon coupon);

    Optional<Coupon> findById(UUID id);
}
