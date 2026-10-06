package com.redsocial.ui.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.api")
public record ApiProperties(String baseUrl) {
}
