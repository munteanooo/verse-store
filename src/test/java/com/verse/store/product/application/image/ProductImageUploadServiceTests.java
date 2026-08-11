package com.verse.store.product.application.image;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.Base64;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

@ExtendWith(MockitoExtension.class)
class ProductImageUploadServiceTests {

    @Mock ProductImageStorage storage;

    @Test
    void uploadsJpegAndUsesServerGeneratedKey() throws Exception {
        var result = service().upload(file("photo.jpg", "image/jpeg", image("jpg")));
        assertThat(result.objectKey()).matches("products/[0-9a-f-]{36}\\.jpg");
        assertThat(result.url()).matches("/media/products/[0-9a-f-]{36}\\.jpg");
        verify(storage).store(eq(result.objectKey()), any(byte[].class), eq("image/jpeg"));
    }

    @Test
    void uploadsPngAndWebp() throws Exception {
        assertThat(service().upload(file("photo.png", "image/png", image("png"))).url()).endsWith(".png");
        byte[] webp = Base64.getDecoder().decode(
                "UklGRjwAAABXRUJQVlA4IDAAAADQAQCdASoCAAIAAUAmJaACdLoB+AADsAD+8ut//NgVzXPv9//S4P0uD9Lg/9KQAAA=");
        assertThat(service().upload(file("photo.webp", "image/webp", webp)).url()).endsWith(".webp");
    }

    @Test
    void rejectsEmptyOversizedFalseMimeAndTraversal() throws Exception {
        assertCode(file("empty.png", "image/png", new byte[0]), "EMPTY_IMAGE");
        assertThatThrownBy(() -> new ProductImageUploadService(storage, 2, 12000)
                .upload(file("large.png", "image/png", image("png"))))
                .isInstanceOf(ProductImageUploadException.class).hasMessageContaining("limit");
        assertCode(file("fake.jpg", "image/jpeg", image("png")), "IMAGE_TYPE_MISMATCH");
        assertCode(file("../photo.png", "image/png", image("png")), "INVALID_FILENAME");
        assertCode(file("page.svg", "image/svg+xml", "<svg/>".getBytes()), "UNSUPPORTED_IMAGE_TYPE");
    }

    @Test
    void translatesUnavailableStorage() throws Exception {
        doThrow(new ProductImageStorageException("down", new RuntimeException()))
                .when(storage).store(any(), any(), any());
        assertThatThrownBy(() -> service().upload(file("photo.png", "image/png", image("png"))))
                .isInstanceOf(ProductImageStorageException.class);
    }

    private ProductImageUploadService service() {
        return new ProductImageUploadService(storage, 5 * 1024 * 1024, 12000);
    }

    private void assertCode(MockMultipartFile file, String code) {
        assertThatThrownBy(() -> service().upload(file))
                .isInstanceOf(ProductImageUploadException.class)
                .extracting(error -> ((ProductImageUploadException) error).getCode())
                .isEqualTo(code);
    }

    private static MockMultipartFile file(String name, String type, byte[] bytes) {
        return new MockMultipartFile("file", name, type, bytes);
    }

    private static byte[] image(String format) throws Exception {
        var output = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB), format, output);
        return output.toByteArray();
    }
}
