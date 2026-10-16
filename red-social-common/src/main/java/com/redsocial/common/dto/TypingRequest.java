package com.redsocial.common.dto;

import jakarta.validation.constraints.NotNull;

public record TypingRequest(
        @NotNull(message = "La conversación es obligatoria")
        Long conversationId,
        boolean typing
) {
}
