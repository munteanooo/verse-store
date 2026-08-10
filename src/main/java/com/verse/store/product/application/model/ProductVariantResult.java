package com.verse.store.product.application.model;

import java.time.Instant;
import java.util.UUID;

public record ProductVariantResult(
        UUID id,
        String sku,
        String size,
        String colorName,
        String colorHex,
        int stockQuantity,
        Instant createdAt,
        Instant updatedAt) {
}
