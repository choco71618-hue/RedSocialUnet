package com.redsocial.api.service;

import com.redsocial.api.config.MediaProperties;
import com.redsocial.api.exception.BadRequestException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

/**
 * Guarda los archivos en el disco del servidor y los sirve vía GET /api/media/{publicId}.
 */
public class LocalMediaStorage implements MediaStorage {

    private static final Logger log = LoggerFactory.getLogger(LocalMediaStorage.class);

    private final Path root;
    private final String publicBaseUrl;

    public LocalMediaStorage(MediaProperties properties) {
        this.root = Path.of(properties.resolvedLocalDir()).toAbsolutePath().normalize();
        this.publicBaseUrl = properties.normalizedPublicBaseUrl();
        try {
            Files.createDirectories(root);
        } catch (IOException ex) {
            throw new IllegalStateException("No se pudo crear el directorio de medios: " + root, ex);
        }
        log.info("MediaStorage local activo en {}", root);
    }

    @Override
    public StoredMedia store(MultipartFile file, String folder) {
        String extension = MediaStorage.extensionFor(file.getContentType());
        String publicId = sanitizeFolder(folder) + "-" + UUID.randomUUID() + extension;
        Path target = root.resolve(publicId).normalize();
        if (!target.startsWith(root)) {
            throw new BadRequestException("Ruta de archivo inválida");
        }
        try (InputStream in = file.getInputStream()) {
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ex) {
            throw new BadRequestException("No se pudo guardar el archivo: " + ex.getMessage());
        }
        return new StoredMedia(urlFor(publicId), publicId);
    }

    @Override
    public Optional<StoredResource> load(String publicId) {
        if (publicId == null || publicId.isBlank()) {
            return Optional.empty();
        }
        Path target = root.resolve(publicId).normalize();
        if (!target.startsWith(root) || !Files.isRegularFile(target)) {
            return Optional.empty();
        }
        Resource resource = new FileSystemResource(target);
        return Optional.of(new StoredResource(resource, contentTypeOf(target)));
    }

    private String urlFor(String publicId) {
        String path = "/api/media/" + publicId;
        if (publicBaseUrl != null) {
            return publicBaseUrl + path;
        }
        try {
            return ServletUriComponentsBuilder.fromCurrentContextPath().path(path).toUriString();
        } catch (IllegalStateException ex) {
            return path;
        }
    }

    private String sanitizeFolder(String folder) {
        String base = folder == null || folder.isBlank() ? "redsocial" : folder;
        return base.replaceAll("[^a-zA-Z0-9_-]", "_");
    }

    private String contentTypeOf(Path target) {
        try {
            String probed = Files.probeContentType(target);
            if (probed != null && !probed.isBlank()) {
                return probed;
            }
        } catch (IOException ignored) {
            // se usa el fallback por extensión
        }
        String name = target.getFileName().toString().toLowerCase(Locale.ROOT);
        if (name.endsWith(".mp4")) return "video/mp4";
        if (name.endsWith(".webm")) return "video/webm";
        if (name.endsWith(".mov")) return "video/quicktime";
        if (name.endsWith(".png")) return "image/png";
        if (name.endsWith(".gif")) return "image/gif";
        if (name.endsWith(".webp")) return "image/webp";
        if (name.endsWith(".jpg") || name.endsWith(".jpeg")) return "image/jpeg";
        return "application/octet-stream";
    }
}
