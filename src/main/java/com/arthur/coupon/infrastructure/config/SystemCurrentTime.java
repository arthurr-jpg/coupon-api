package com.arthur.coupon.infrastructure.config;

import com.arthur.coupon.application.port.CurrentTime;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;

@Component
public class SystemCurrentTime implements CurrentTime {

    private final Clock clock;

    public SystemCurrentTime(Clock clock) {
        this.clock = clock;
    }

    @Override
    public Instant now() {
        return clock.instant();
    }
}
