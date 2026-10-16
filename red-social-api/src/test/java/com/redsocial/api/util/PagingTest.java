package com.redsocial.api.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PagingTest {

    @Test
    void normalizaPaginaNegativaYTamanoInvalido() {
        var pageable = Paging.of(-5, 0);
        assertThat(pageable.getPageNumber()).isZero();
        assertThat(pageable.getPageSize()).isEqualTo(Paging.DEFAULT_SIZE);
    }

    @Test
    void acotaElTamanoAlMaximo() {
        var pageable = Paging.of(2, 10_000);
        assertThat(pageable.getPageNumber()).isEqualTo(2);
        assertThat(pageable.getPageSize()).isEqualTo(Paging.MAX_SIZE);
    }

    @Test
    void respetaValoresValidos() {
        var pageable = Paging.of(3, 25);
        assertThat(pageable.getPageNumber()).isEqualTo(3);
        assertThat(pageable.getPageSize()).isEqualTo(25);
    }
}
