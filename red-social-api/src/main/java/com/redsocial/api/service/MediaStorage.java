package com.redsocial.api.service;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

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

    record StoredMedia(String url, String publicId) {
    }

    record StoredResource(Resource resource, String contentType) {
    }
}
