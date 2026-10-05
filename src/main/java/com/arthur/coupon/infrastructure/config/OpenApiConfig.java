package com.arthur.coupon.infrastructure.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI couponOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Coupon API")
                        .version("1.0.0")
                        .description("""
                                API para cadastro, consulta e remocao (soft delete) de cupons.

                                **Regras de negocio**
                                - `code`, `description`, `discountValue` e `expirationDate` sao obrigatorios.
                                - Caracteres especiais do `code` sao removidos; o resultado deve ter exatamente 6 caracteres alfanumericos.
                                - `discountValue` minimo de 0.5, sem maximo.
                                - `expirationDate` nunca pode estar no passado.
                                - Um cupom pode ser criado ja publicado.
                                - A delecao e logica (soft delete) e nao pode ser repetida.
                                """))
                .addServersItem(new Server().url("http://localhost:8080").description("Local"));
    }
}
