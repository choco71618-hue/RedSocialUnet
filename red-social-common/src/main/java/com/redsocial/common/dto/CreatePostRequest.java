package com.redsocial.common.dto;

public record CreatePostRequest(
        String content,
        String mediaUrl
) {
    public CreatePostRequest(String content) {
        this(content, null);
    }
}