package com.redsocial.api.service;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.util.Locale;
import java.util.Optional;

/**
 * Puerto de almacenamiento de medios. Permite intercambiar el backend
 * (S3, disco local, ...) sin tocar la lógica de negocio.
 */
public interface MediaStorage {

    StoredMedia store(MultipartFile file, String folder);

    default Optional<StoredResource> load(String publicId) {
        return Optional.empty();
    }

    /**
     * Extensión segura derivada del content-type validado (nunca del nombre
     * original del archivo, que el cliente controla).
     */
    static String extensionFor(String contentType) {
        if (contentType == null) {
            return ".bin";
        }
        String type = contentType.toLowerCase(Locale.ROOT).split(";")[0].trim();
        return switch (type) {
            case "image/png" -> ".png";
            case "image/jpeg", "image/jpg" -> ".jpg";
            case "image/gif" -> ".gif";
            case "image/webp" -> ".webp";
            case "image/avif" -> ".avif";
            case "video/mp4" -> ".mp4";
            case "video/webm" -> ".webm";
            case "video/quicktime" -> ".mov";
            default -> ".bin";
        };
    }

    record StoredMedia(String url, String publicId) {
    }

    record StoredResource(Resource resource, String contentType) {
    }
}
