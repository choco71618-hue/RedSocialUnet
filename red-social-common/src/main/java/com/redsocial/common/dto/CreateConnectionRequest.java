package com.redsocial.common.dto;

import jakarta.validation.constraints.NotNull;

public record CreateConnectionRequest(
        @NotNull(message = "El preparador es obligatorio")
        Long tutorId
) {
}
