package com.redsocial.api.service;

import com.redsocial.api.config.MediaProperties;
import com.redsocial.api.exception.BadRequestException;
import com.redsocial.api.exception.ResourceNotFoundException;
import com.redsocial.common.dto.MediaDto;
import com.redsocial.common.dto.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class MediaService {

    private final MediaStorage storage;
    private final MediaProperties properties;

    public MediaService(MediaStorage storage, MediaProperties properties) {
        this.storage = storage;
        this.properties = properties;
    }

    public MediaDto upload(MultipartFile file, String folder) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("El archivo está vacío");
        }
        MediaType mediaType = resolveType(file.getContentType());
        long maxBytes = mediaType == MediaType.VIDEO
                ? properties.resolvedMaxVideoBytes()
                : properties.resolvedMaxImageBytes();
        if (file.getSize() > maxBytes) {
            throw new BadRequestException(
                    "El archivo supera el máximo permitido (" + (maxBytes / (1024 * 1024)) + " MB)");
        }
        MediaStorage.StoredMedia stored = storage.store(file, folder);
        return new MediaDto(stored.url(), stored.publicId(), mediaType);
    }

    public MediaStorage.StoredResource load(String publicId) {
        return storage.load(publicId)
                .orElseThrow(() -> new ResourceNotFoundException("Archivo no encontrado"));
    }

    private MediaType resolveType(String contentType) {
        if (contentType == null) {
            throw new BadRequestException("No se pudo determinar el tipo de archivo");
        }
        if (contentType.startsWith("image/")) {
            return MediaType.IMAGE;
        }
        if (contentType.startsWith("video/")) {
            return MediaType.VIDEO;
        }
        throw new BadRequestException("Solo se permiten imágenes o videos");
    }
}
