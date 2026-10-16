package com.redsocial.api.service;

import com.cloudinary.Cloudinary;
import com.redsocial.api.config.CloudinaryProperties;
import com.redsocial.api.exception.BadRequestException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Sube los archivos a Cloudinary y devuelve la URL segura (CDN) del recurso.
 */
public class CloudinaryMediaStorage implements MediaStorage {

    private static final Logger log = LoggerFactory.getLogger(CloudinaryMediaStorage.class);

    private final Cloudinary cloudinary;
    private final String baseFolder;

    public CloudinaryMediaStorage(CloudinaryProperties properties) {
        Map<String, Object> config = new HashMap<>();
        config.put("cloud_name", properties.resolvedCloudName());
        config.put("api_key", properties.resolvedApiKey());
        config.put("api_secret", properties.resolvedApiSecret());
        config.put("secure", true);
        this.cloudinary = new Cloudinary(config);
        this.baseFolder = properties.resolvedFolder();
        log.info("MediaStorage Cloudinary activo (cloud={}, folder={})",
                properties.resolvedCloudName(), baseFolder);
    }

    @Override
    @SuppressWarnings("unchecked")
    public StoredMedia store(MultipartFile file, String folder) {
        Map<String, Object> options = new HashMap<>();
        options.put("resource_type", "auto");
        options.put("folder", targetFolder(folder));
        options.put("use_filename", true);
        options.put("unique_filename", true);
        options.put("overwrite", false);
        try {
            Map<String, Object> result = cloudinary.uploader().upload(file.getBytes(), options);
            String url = (String) result.get("secure_url");
            String publicId = (String) result.get("public_id");
            return new StoredMedia(url, publicId);
        } catch (IOException ex) {
            throw new BadRequestException("No se pudo leer el archivo: " + ex.getMessage());
        } catch (RuntimeException ex) {
            throw new BadRequestException("No se pudo subir el archivo a Cloudinary: " + ex.getMessage());
        }
    }

    private String targetFolder(String folder) {
        String sanitized = folder == null ? "" : folder.trim().replaceAll("[^a-zA-Z0-9_-]", "_");
        if (sanitized.isBlank()) {
            return baseFolder;
        }
        if (sanitized.toLowerCase(Locale.ROOT).equals(baseFolder.toLowerCase(Locale.ROOT))) {
            return sanitized;
        }
        return baseFolder + "/" + sanitized;
    }
}
