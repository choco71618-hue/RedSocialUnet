package com.redsocial.common.dto;

import jakarta.validation.constraints.NotNull;

public record StartConversationRequest(
        @NotNull(message = "El destinatario es obligatorio")
        Long recipientId
) {
}
