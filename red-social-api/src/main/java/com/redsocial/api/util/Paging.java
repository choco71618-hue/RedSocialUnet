package com.redsocial.api.util;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

/**
 * Construye {@link Pageable} saneando la entrada del cliente: la página nunca
 * es negativa y el tamaño queda acotado a un máximo razonable.
 */
public final class Paging {

    public static final int MAX_SIZE = 100;
    public static final int DEFAULT_SIZE = 20;

    private Paging() {
    }

    public static Pageable of(int page, int size) {
        int safePage = Math.max(0, page);
        int safeSize = size <= 0 ? DEFAULT_SIZE : Math.min(size, MAX_SIZE);
        return PageRequest.of(safePage, safeSize);
    }
}
