package com.verse.store.product.application.image;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;

import javax.imageio.ImageIO;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ProductImageUploadService {

    private static final Map<String, String> EXTENSIONS = Map.of(
            "image/jpeg", "jpg", "image/png", "png", "image/webp", "webp");
    private final ProductImageStorage storage;
    private final long maximumBytes;
    private final int maximumDimension;

    public ProductImageUploadService(
            ProductImageStorage storage,
            @Value("${app.product-images.max-bytes:5242880}") long maximumBytes,
            @Value("${app.product-images.max-dimension:12000}") int maximumDimension) {
        this.storage = storage;
        this.maximumBytes = maximumBytes;
        this.maximumDimension = maximumDimension;
    }

    public ProductImageUploadResult upload(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw invalid(HttpStatus.UNPROCESSABLE_CONTENT, "EMPTY_IMAGE", "Choose a non-empty image file");
        }
        String filename = file.getOriginalFilename();
        if (filename != null && (filename.contains("..") || filename.contains("/") || filename.contains("\\"))) {
            throw invalid(HttpStatus.BAD_REQUEST, "INVALID_FILENAME", "The image filename is invalid");
        }
        String declaredType = file.getContentType();
        if (!EXTENSIONS.containsKey(declaredType)) {
            throw invalid(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "UNSUPPORTED_IMAGE_TYPE",
                    "Only JPEG, PNG and WebP images are supported");
        }
        if (file.getSize() > maximumBytes) {
            throw invalid(HttpStatus.PAYLOAD_TOO_LARGE, "IMAGE_TOO_LARGE", "The image exceeds the upload limit");
        }
        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException exception) {
            throw invalid(HttpStatus.UNPROCESSABLE_CONTENT, "INVALID_IMAGE", "The image could not be read");
        }
        if (bytes.length == 0) {
            throw invalid(HttpStatus.UNPROCESSABLE_CONTENT, "EMPTY_IMAGE", "Choose a non-empty image file");
        }
        if (bytes.length > maximumBytes) {
            throw invalid(HttpStatus.PAYLOAD_TOO_LARGE, "IMAGE_TOO_LARGE", "The image exceeds the upload limit");
        }
        String actualType = detectType(bytes);
        if (!declaredType.equals(actualType)) {
            throw invalid(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "IMAGE_TYPE_MISMATCH",
                    "The file content does not match its declared image type");
        }
        validateDecodable(bytes);
        String objectKey = "products/" + UUID.randomUUID() + "." + EXTENSIONS.get(actualType);
        storage.store(objectKey, bytes, actualType);
        return new ProductImageUploadResult(objectKey, "/media/products/" + objectKey.substring("products/".length()));
    }

    private void validateDecodable(byte[] bytes) {
        try {
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(bytes));
            if (image == null || image.getWidth() < 1 || image.getHeight() < 1
                    || image.getWidth() > maximumDimension || image.getHeight() > maximumDimension) {
                throw invalid(HttpStatus.UNPROCESSABLE_CONTENT, "INVALID_IMAGE",
                        "The image cannot be decoded or has unreasonable dimensions");
            }
        } catch (IOException exception) {
            throw invalid(HttpStatus.UNPROCESSABLE_CONTENT, "INVALID_IMAGE", "The image cannot be decoded");
        }
    }

    private String detectType(byte[] bytes) {
        if (bytes.length >= 3 && (bytes[0] & 0xff) == 0xff && (bytes[1] & 0xff) == 0xd8
                && (bytes[2] & 0xff) == 0xff) return "image/jpeg";
        if (bytes.length >= 8 && (bytes[0] & 0xff) == 0x89 && bytes[1] == 'P'
                && bytes[2] == 'N' && bytes[3] == 'G' && bytes[4] == 0x0d
                && bytes[5] == 0x0a && bytes[6] == 0x1a && bytes[7] == 0x0a) return "image/png";
        if (bytes.length >= 12 && bytes[0] == 'R' && bytes[1] == 'I' && bytes[2] == 'F'
                && bytes[3] == 'F' && bytes[8] == 'W' && bytes[9] == 'E'
                && bytes[10] == 'B' && bytes[11] == 'P') return "image/webp";
        return null;
    }

    private ProductImageUploadException invalid(HttpStatus status, String code, String message) {
        return new ProductImageUploadException(status, code, message);
    }
}
