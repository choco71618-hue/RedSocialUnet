package com.redsocial.common.dto;

import java.time.Instant;

public record UserDto(
        Long id,
        String username,
        String email,
        String displayName,
        String bio,
        String avatarUrl,
        String coverUrl,
        boolean following,
        long followersCount,
        long followingCount,
        long postsCount,
        Instant createdAt
) {
}