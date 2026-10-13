package com.redsocial.common.dto;

public record TypingEvent(
        Long conversationId,
        String username,
        boolean typing
) {
}