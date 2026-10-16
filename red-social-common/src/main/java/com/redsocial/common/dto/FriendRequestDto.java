package com.redsocial.common.dto;

import java.time.Instant;

public record FriendRequestDto(
        Long id,
        UserDto user,
        String status,
        boolean incoming,
        Instant createdAt
) {
}
