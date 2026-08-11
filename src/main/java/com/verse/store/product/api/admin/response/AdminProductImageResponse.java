package com.verse.store.product.api.admin.response;

import java.time.Instant;
import java.util.UUID;

public record AdminProductImageResponse(
        UUID id,
        String url,
        String altText,
        int displayOrder,
        boolean primaryImage,
        Instant createdAt) {
}
