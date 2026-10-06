package com.redsocial.common.dto;

import java.time.Instant;

public record CommentDto(
        Long id,
        Long postId,
        Long authorId,
        String authorUsername,
        String authorDisplayName,
        String authorAvatarUrl,
        String content,
        Instant createdAt
) {
}
