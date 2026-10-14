package com.redsocial.common.dto;

public record MediaDto(
        String url,
        String publicId,
        MediaType mediaType
) {
    public MediaDto(String url, String publicId) {
        this(url, publicId, MediaType.IMAGE);
    }
}
