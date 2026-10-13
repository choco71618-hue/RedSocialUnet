package com.redsocial.common.dto;

import java.time.Instant;

public record NotificationDto(
        Long id,
        NotificationType type,
        Long actorId,
        String actorUsername,
        String actorDisplayName,
        String actorAvatarUrl,
        Long targetId,
        String text,
        boolean read,
        Instant createdAt
) {
}