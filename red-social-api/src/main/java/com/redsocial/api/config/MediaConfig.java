package com.redsocial.api.config;

import com.redsocial.api.service.LocalMediaStorage;
import com.redsocial.api.service.MediaStorage;
import com.redsocial.api.service.S3MediaStorage;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MediaConfig {

    @Bean
    public MediaStorage mediaStorage(MediaProperties media, S3Properties s3) {
        String mode = media.storage() == null ? "auto" : media.storage().trim();
        if ("s3".equalsIgnoreCase(mode)) {
            if (!s3.isConfigured()) {
                throw new IllegalStateException(
                        "app.media.storage=s3 pero faltan credenciales de S3 (bucket/access-key/secret-key)");
            }
            return new S3MediaStorage(s3);
        }
        if ("local".equalsIgnoreCase(mode)) {
            return new LocalMediaStorage(media);
        }
        return s3.isConfigured() ? new S3MediaStorage(s3) : new LocalMediaStorage(media);
    }
}
