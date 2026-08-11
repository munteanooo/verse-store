package com.verse.store.product.api.admin.response;

import java.time.Instant;
import java.util.UUID;

public record AdminProductVariantResponse(
        UUID id,
        String sku,
        String size,
        String colorName,
        String colorHex,
        int stockQuantity,
        Instant createdAt,
        Instant updatedAt) {
}
