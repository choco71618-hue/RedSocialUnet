package com.redsocial.common.dto;

import com.redsocial.common.dto.ProjectKind;
import com.redsocial.common.dto.ProjectStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CreateProjectRequest(
        ProjectKind kind,
        String title,
        String description,
        LocalDateTime eventDate,
        String location,
        BigDecimal targetAmount,
        ProjectStatus status
) {
}