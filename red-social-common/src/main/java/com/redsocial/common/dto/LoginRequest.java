package com.redsocial.common.dto;

public record LoginRequest(
        String usernameOrEmail,
        String password
) {
}
