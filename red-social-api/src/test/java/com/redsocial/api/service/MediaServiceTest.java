package com.redsocial.api.service;

import com.redsocial.api.config.MediaProperties;
import com.redsocial.api.exception.BadRequestException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class MediaServiceTest {

    private final MediaService service = new MediaService(
            mock(MediaStorage.class),
            new MediaProperties("local", "uploads", null, 0, 0));

    @Test
    void rechazaSvg() {
        var file = new MockMultipartFile("file", "logo.svg", "image/svg+xml", "<svg/>".getBytes());
        assertThatThrownBy(() -> service.upload(file, "redsocial"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("SVG");
    }

    @Test
    void rechazaImagenNoPermitida() {
        var file = new MockMultipartFile("file", "doc.bmp", "image/bmp", new byte[]{1, 2, 3});
        assertThatThrownBy(() -> service.upload(file, "redsocial"))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void rechazaTipoNoMedia() {
        var file = new MockMultipartFile("file", "doc.pdf", "application/pdf", new byte[]{1});
        assertThatThrownBy(() -> service.upload(file, "redsocial"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("imágenes o videos");
    }

    @Test
    void derivarExtensionSeguraDesdeContentType() {
        assertThat(MediaStorage.extensionFor("image/png")).isEqualTo(".png");
        assertThat(MediaStorage.extensionFor("image/svg+xml")).isEqualTo(".bin");
        assertThat(MediaStorage.extensionFor("video/mp4")).isEqualTo(".mp4");
        assertThat(MediaStorage.extensionFor("text/html")).isEqualTo(".bin");
    }
}
