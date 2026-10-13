package com.redsocial.common.dto;

import java.math.BigDecimal;

public record DonateRequest(
        BigDecimal amount
) {
}