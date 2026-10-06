package com.redsocial.common.dto;

import java.time.Instant;

public record ConversationDto(
        Long id,
        UserDto otherUser,
        String lastMessage,
        Instant lastMessageAt,
        long unreadCount
) {
}
