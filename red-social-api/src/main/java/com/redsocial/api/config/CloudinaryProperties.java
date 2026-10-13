package com.redsocial.api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.cloudinary")
public record CloudinaryProperties(
        String url,
        String cloudName,
        String apiKey,
        String apiSecret
) {

    public boolean isConfigured() {
        boolean urlConfigured = url != null && !url.isBlank();
        boolean partsConfigured = cloudName != null && !cloudName.isBlank()
                && apiKey != null && !apiKey.isBlank()
                && apiSecret != null && !apiSecret.isBlank();
        return urlConfigured || partsConfigured;
    }
}