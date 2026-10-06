package com.redsocial.common.dto;

public record SendMessageRequest(
        Long conversationId,
        Long recipientId,
        String content
) {
}
