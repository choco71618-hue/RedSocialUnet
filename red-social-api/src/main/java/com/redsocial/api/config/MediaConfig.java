package com.redsocial.api.config;

import com.redsocial.api.service.CloudinaryMediaStorage;
import com.redsocial.api.service.LocalMediaStorage;
import com.redsocial.api.service.MediaStorage;
import com.redsocial.api.service.S3MediaStorage;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MediaConfig {

    @Bean
    public MediaStorage mediaStorage(MediaProperties media, S3Properties s3, CloudinaryProperties cloudinary) {
        String mode = media.storage() == null ? "auto" : media.storage().trim();
        if ("cloudinary".equalsIgnoreCase(mode)) {
            requireCloudinary(cloudinary);
            return new CloudinaryMediaStorage(cloudinary);
        }
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
        // auto: Cloudinary es el sistema principal, luego S3, luego disco local
        if (cloudinary.isConfigured()) {
            return new CloudinaryMediaStorage(cloudinary);
        }
        return s3.isConfigured() ? new S3MediaStorage(s3) : new LocalMediaStorage(media);
    }

    private void requireCloudinary(CloudinaryProperties cloudinary) {
        if (!cloudinary.isConfigured()) {
            throw new IllegalStateException(
                    "app.media.storage=cloudinary pero faltan credenciales (cloud-name/api-key/api-secret o CLOUDINARY_URL)");
        }
    }
}
