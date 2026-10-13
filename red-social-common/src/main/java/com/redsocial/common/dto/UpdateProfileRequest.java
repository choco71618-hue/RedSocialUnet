package com.redsocial.common.dto;

public record UpdateProfileRequest(
        String displayName,
        String bio,
        String avatarUrl,
        String coverUrl
) {
    public UpdateProfileRequest(String displayName, String bio, String avatarUrl) {
        this(displayName, bio, avatarUrl, null);
    }
}