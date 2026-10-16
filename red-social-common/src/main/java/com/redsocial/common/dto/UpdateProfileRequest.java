package com.redsocial.common.dto;

public record UpdateProfileRequest(
        String displayName,
        String bio,
        String avatarUrl,
        String coverUrl,
        String career,
        String subjectOfInterest,
        TutorType tutorType
) {
    public UpdateProfileRequest(String displayName, String bio, String avatarUrl) {
        this(displayName, bio, avatarUrl, null, null, null, null);
    }

    public UpdateProfileRequest(String displayName, String bio, String avatarUrl, String coverUrl) {
        this(displayName, bio, avatarUrl, coverUrl, null, null, null);
    }
}
