package com.redsocial.common.dto;

public record UpdateProfileRequest(
        String displayName,
        String bio,
        String avatarUrl
) {
}
