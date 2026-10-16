package com.redsocial.api.service;

import com.redsocial.api.config.S3Properties;
import com.redsocial.api.exception.BadRequestException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.net.URI;
import java.util.UUID;

/**
 * Guarda los archivos en un bucket S3 (AWS o compatible, p. ej. MinIO) y
 * devuelve la URL pública del objeto.
 */
public class S3MediaStorage implements MediaStorage {

    private static final Logger log = LoggerFactory.getLogger(S3MediaStorage.class);

    private final S3Client client;
    private final String bucket;
    private final String publicBaseUrl;

    public S3MediaStorage(S3Properties properties) {
        this.bucket = properties.bucket();
        this.publicBaseUrl = properties.normalizedPublicBaseUrl();

        var builder = S3Client.builder()
                .region(Region.of(properties.resolvedRegion()))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(properties.accessKey(), properties.secretKey())))
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(properties.pathStyleAccess())
                        .build());
        if (properties.endpoint() != null && !properties.endpoint().isBlank()) {
            builder.endpointOverride(URI.create(properties.endpoint().trim()));
        }
        this.client = builder.build();
        log.info("MediaStorage S3 activo (bucket={}, region={})", bucket, properties.resolvedRegion());
    }

    @Override
    public StoredMedia store(MultipartFile file, String folder) {
        String key = sanitizeFolder(folder) + "/" + UUID.randomUUID()
                + MediaStorage.extensionFor(file.getContentType());
        try {
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .contentType(file.getContentType())
                    .build();
            client.putObject(request, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));
        } catch (IOException ex) {
            throw new BadRequestException("No se pudo leer el archivo: " + ex.getMessage());
        } catch (RuntimeException ex) {
            throw new BadRequestException("No se pudo subir el archivo a S3: " + ex.getMessage());
        }
        String url = publicBaseUrl != null
                ? publicBaseUrl + "/" + key
                : client.utilities().getUrl(b -> b.bucket(bucket).key(key)).toString();
        return new StoredMedia(url, key);
    }

    private String sanitizeFolder(String folder) {
        String base = folder == null || folder.isBlank() ? "redsocial" : folder;
        return base.replaceAll("[^a-zA-Z0-9_/-]", "_");
    }
}
