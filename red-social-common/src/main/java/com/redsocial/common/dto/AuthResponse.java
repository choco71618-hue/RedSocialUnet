package com.redsocial.common.dto;

public record AuthResponse(
        String token,
        String tokenType,
        long expiresInSeconds,
        UserDto user
) {
}
