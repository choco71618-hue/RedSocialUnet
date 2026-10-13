package com.redsocial.common.dto;

import com.redsocial.common.dto.ProjectKind;
import com.redsocial.common.dto.ProjectStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;

public record ProjectDto(
        Long id,
        ProjectKind kind,
        String title,
        String description,
        Long creatorId,
        String creatorUsername,
        String creatorDisplayName,
        String creatorAvatarUrl,
        LocalDateTime eventDate,
        String location,
        BigDecimal targetAmount,
        BigDecimal raisedAmount,
        ProjectStatus status,
        long donationCount,
        Instant createdAt
) {
}