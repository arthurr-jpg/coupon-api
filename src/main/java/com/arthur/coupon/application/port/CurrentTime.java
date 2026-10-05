package com.arthur.coupon.application.port;

import java.time.Instant;

/**
 * Porta de saida: a aplicacao pede "que horas sao" sem saber de onde isso vem.
 */
public interface CurrentTime {

    Instant now();
}
