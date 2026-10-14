package com.redsocial.api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.s3")
public record S3Properties(
        boolean enabled,
        String bucket,
        String region,
        String endpoint,
        String accessKey,
        String secretKey,
        String publicBaseUrl,
        boolean pathStyleAccess
) {

    public boolean isConfigured() {
        return enabled
                && bucket != null && !bucket.isBlank()
                && accessKey != null && !accessKey.isBlank()
                && secretKey != null && !secretKey.isBlank();
    }

    public String resolvedRegion() {
        return region == null || region.isBlank() ? "us-east-1" : region.trim();
    }

    public String normalizedPublicBaseUrl() {
        if (publicBaseUrl == null || publicBaseUrl.isBlank()) {
            return null;
        }
        String trimmed = publicBaseUrl.trim();
        return trimmed.endsWith("/") ? trimmed.substring(0, trimmed.length() - 1) : trimmed;
    }
}
