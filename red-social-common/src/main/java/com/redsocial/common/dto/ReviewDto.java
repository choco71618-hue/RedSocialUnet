package com.redsocial.common.dto;

import java.time.Instant;

public record ReviewDto(
        Long id,
        Long connectionId,
        Long studentId,
        String studentUsername,
        String studentDisplayName,
        Long tutorId,
        int rating,
        String comment,
        Instant createdAt
) {
}
