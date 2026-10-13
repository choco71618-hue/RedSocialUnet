package com.redsocial.common.dto;

import java.time.Instant;

public record PostDto(
        Long id,
        Long authorId,
        String authorUsername,
        String authorDisplayName,
        String authorAvatarUrl,
        String content,
        String mediaUrl,
        Instant createdAt,
        Instant updatedAt,
        long likeCount,
        long commentCount,
        boolean likedByMe
) {
}
