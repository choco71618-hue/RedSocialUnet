package com.redsocial.common.dto;

public record TypingRequest(
        Long conversationId,
        boolean typing
) {
}