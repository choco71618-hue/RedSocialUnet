package com.redsocial.common.dto;

import java.util.List;

public record TutorDto(
        Long id,
        String username,
        String displayName,
        String avatarUrl,
        String bio,
        TutorType tutorType,
        List<TutorSubjectDto> subjects,
        double averageRating,
        long reviewCount,
        ConnectionStatus connectionStatus
) {
}
