package com.redsocial.api.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.redsocial.api.config.CloudinaryProperties;
import com.redsocial.api.exception.BadRequestException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@Service
public class MediaService {

    private static final long MAX_BYTES = 8L * 1024 * 1024;

    private final Cloudinary cloudinary;

    public MediaService(CloudinaryProperties properties) {
        if (properties.url() != null && !properties.url().isBlank()) {
            this.cloudinary = new Cloudinary(properties.url());
        } else if (properties.isConfigured()) {
            this.cloudinary = new Cloudinary(ObjectUtils.asMap(
                    "cloud_name", properties.cloudName(),
                    "api_key", properties.apiKey(),
                    "api_secret", properties.apiSecret()));
        } else {
            this.cloudinary = null;
        }
    }

    public UrlInfo upload(MultipartFile file, String folder) {
        if (cloudinary == null) {
            throw new BadRequestException("Cloudinary no está configurado (CLOUDINARY_URL)");
        }
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("El archivo está vacío");
        }
        if (file.getSize() > MAX_BYTES) {
            throw new BadRequestException("El archivo no puede superar los 8 MB");
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new BadRequestException("Solo se permiten imágenes");
        }
        try {
            Map<?, ?> result = cloudinary.uploader().upload(file.getBytes(),
                    ObjectUtils.asMap("folder", folder, "resource_type", "image"));
            String url = (String) result.get("secure_url");
            String publicId = (String) result.get("public_id");
            if (url == null || publicId == null) {
                throw new BadRequestException("La imagen no pudo procesarse");
            }
            return new UrlInfo(url, publicId);
        } catch (IOException ex) {
            throw new BadRequestException("No se pudo subir la imagen: " + ex.getMessage());
        }
    }

    public record UrlInfo(String url, String publicId) {
    }
}