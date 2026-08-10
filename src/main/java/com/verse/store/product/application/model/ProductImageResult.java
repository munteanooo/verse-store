package com.verse.store.product.application.model;

import java.time.Instant;
import java.util.UUID;

public record ProductImageResult(
        UUID id,
        String url,
        String altText,
        int displayOrder,
        boolean primaryImage,
        Instant createdAt) {
}
