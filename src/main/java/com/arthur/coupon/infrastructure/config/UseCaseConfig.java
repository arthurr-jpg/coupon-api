package com.arthur.coupon.infrastructure.config;

import com.arthur.coupon.application.port.CouponRepository;
import com.arthur.coupon.application.port.CurrentTime;
import com.arthur.coupon.application.usecase.CreateCouponUseCase;
import com.arthur.coupon.application.usecase.DeleteCouponUseCase;
import com.arthur.coupon.application.usecase.FindCouponUseCase;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * A camada application nao conhece o Spring: quem monta os use cases e a infraestrutura.
 */
@Configuration
public class UseCaseConfig {

    @Bean
    public CreateCouponUseCase createCouponUseCase(CouponRepository repository, CurrentTime currentTime) {
        return new CreateCouponUseCase(repository, currentTime);
    }

    @Bean
    public FindCouponUseCase findCouponUseCase(CouponRepository repository) {
        return new FindCouponUseCase(repository);
    }

    @Bean
    public DeleteCouponUseCase deleteCouponUseCase(CouponRepository repository, CurrentTime currentTime) {
        return new DeleteCouponUseCase(repository, currentTime);
    }
}
