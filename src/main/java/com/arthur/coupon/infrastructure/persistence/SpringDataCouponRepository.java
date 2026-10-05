package com.arthur.coupon.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

interface SpringDataCouponRepository extends JpaRepository<CouponJpaEntity, UUID> {
}
