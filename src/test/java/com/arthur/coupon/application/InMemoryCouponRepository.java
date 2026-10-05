package com.arthur.coupon.application;

import com.arthur.coupon.application.port.CouponRepository;
import com.arthur.coupon.domain.Coupon;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Implementacao em memoria da porta. Guarda copias, como um banco real: alterar o objeto
 * retornado sem chamar save() nao muda o que esta armazenado.
 */
public class InMemoryCouponRepository implements CouponRepository {

    private final Map<UUID, Coupon> store = new HashMap<>();

    @Override
    public Coupon save(Coupon coupon) {
        store.put(coupon.getId(), copyOf(coupon));
        return coupon;
    }

    @Override
    public Optional<Coupon> findById(UUID id) {
        return Optional.ofNullable(store.get(id)).map(InMemoryCouponRepository::copyOf);
    }

    public int count() {
        return store.size();
    }

    private static Coupon copyOf(Coupon c) {
        return Coupon.restore(c.getId(), c.getCode(), c.getDescription(), c.getDiscountValue(),
                c.getExpirationDate(), c.getStatus(), c.isPublished(), c.isRedeemed(), c.getDeletedAt(), c.getVersion());
    }
}
