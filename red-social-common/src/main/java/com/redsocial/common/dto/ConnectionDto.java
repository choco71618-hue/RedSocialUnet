package com.redsocial.common.dto;

import java.time.Instant;

public record ConnectionDto(
        Long id,
        Long studentId,
        String studentUsername,
        String studentDisplayName,
        Long tutorId,
        String tutorUsername,
        String tutorDisplayName,
        ConnectionStatus status,
        String reviewCode,
        Instant createdAt,
        Instant acceptedAt
) {
}
