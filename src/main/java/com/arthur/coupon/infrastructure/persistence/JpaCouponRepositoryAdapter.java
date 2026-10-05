package com.arthur.coupon.infrastructure.persistence;

import com.arthur.coupon.application.port.CouponRepository;
import com.arthur.coupon.domain.Coupon;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public class JpaCouponRepositoryAdapter implements CouponRepository {

    private final SpringDataCouponRepository jpaRepository;

    JpaCouponRepositoryAdapter(SpringDataCouponRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Coupon save(Coupon coupon) {
        return jpaRepository.save(CouponJpaEntity.fromDomain(coupon)).toDomain();
    }

    @Override
    public Optional<Coupon> findById(UUID id) {
        return jpaRepository.findById(id).map(CouponJpaEntity::toDomain);
    }
}
