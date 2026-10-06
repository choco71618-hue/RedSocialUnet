package com.redsocial.common.dto;

import java.time.Instant;

public record MessageDto(
        Long id,
        Long conversationId,
        Long senderId,
        String senderUsername,
        String senderDisplayName,
        String content,
        Instant createdAt
) {
}
