package com.redsocial.api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.media")
public record MediaProperties(
        String storage,
        String localDir,
        String publicBaseUrl,
        long maxImageBytes,
        long maxVideoBytes
) {

    public String resolvedLocalDir() {
        return localDir == null || localDir.isBlank() ? "uploads" : localDir;
    }

    public long resolvedMaxImageBytes() {
        return maxImageBytes > 0 ? maxImageBytes : 8L * 1024 * 1024;
    }

    public long resolvedMaxVideoBytes() {
        return maxVideoBytes > 0 ? maxVideoBytes : 50L * 1024 * 1024;
    }

    public String normalizedPublicBaseUrl() {
        if (publicBaseUrl == null || publicBaseUrl.isBlank()) {
            return null;
        }
        String trimmed = publicBaseUrl.trim();
        return trimmed.endsWith("/") ? trimmed.substring(0, trimmed.length() - 1) : trimmed;
    }
}
