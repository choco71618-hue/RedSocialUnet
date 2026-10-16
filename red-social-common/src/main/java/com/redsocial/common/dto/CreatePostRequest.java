package com.redsocial.common.dto;

import jakarta.validation.constraints.Size;

public record CreatePostRequest(
        @Size(max = 5000, message = "El contenido no puede superar los 5000 caracteres")
        String content,
        @Size(max = 2048, message = "La URL del contenido es demasiado larga")
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
