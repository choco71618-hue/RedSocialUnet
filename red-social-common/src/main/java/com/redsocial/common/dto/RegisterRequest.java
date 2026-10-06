package com.redsocial.common.dto;

public record RegisterRequest(
        String username,
        String email,
        String password,
        String displayName
) {
}
