package com.redsocial.common.dto;

public record CreatePostRequest(
        String content,
        String mediaUrl,
        MediaType mediaType
) {
    public CreatePostRequest(String content) {
        this(content, null, null);
    }

    public CreatePostRequest(String content, String mediaUrl) {
        this(content, mediaUrl, null);
    }
}
