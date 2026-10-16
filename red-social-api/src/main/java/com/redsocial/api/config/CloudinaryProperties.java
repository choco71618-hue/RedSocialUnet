package com.redsocial.api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.net.URI;

/**
 * Configuración de Cloudinary. Acepta credenciales explícitas
 * (cloud-name / api-key / api-secret) o una única CLOUDINARY_URL con el
 * formato cloudinary://api_key:api_secret@cloud_name.
 */
@ConfigurationProperties(prefix = "app.cloudinary")
public record CloudinaryProperties(
        String url,
        String cloudName,
        String apiKey,
        String apiSecret,
        String folder
) {

    public String resolvedCloudName() {
        if (isNotBlank(cloudName)) {
            return cloudName.trim();
        }
        return parseUrlPart(3);
    }

    public String resolvedApiKey() {
        if (isNotBlank(apiKey)) {
            return apiKey.trim();
        }
        return parseUrlPart(1);
    }

    public String resolvedApiSecret() {
        if (isNotBlank(apiSecret)) {
            return apiSecret.trim();
        }
        return parseUrlPart(2);
    }

    public String resolvedFolder() {
        return isNotBlank(folder) ? folder.trim() : "redsocial";
    }

    public boolean isConfigured() {
        return isNotBlank(resolvedCloudName())
                && isNotBlank(resolvedApiKey())
                && isNotBlank(resolvedApiSecret());
    }

    private String parseUrlPart(int index) {
        if (!isNotBlank(url)) {
            return null;
        }
        try {
            URI uri = URI.create(url.trim());
            return switch (index) {
                case 1 -> uri.getUserInfo() == null ? null : uri.getUserInfo().split(":", 2)[0];
                case 2 -> {
                    String[] parts = uri.getUserInfo() == null ? new String[0] : uri.getUserInfo().split(":", 2);
                    yield parts.length == 2 ? parts[1] : null;
                }
                case 3 -> uri.getHost();
                default -> null;
            };
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private static boolean isNotBlank(String value) {
        return value != null && !value.isBlank();
    }
}
