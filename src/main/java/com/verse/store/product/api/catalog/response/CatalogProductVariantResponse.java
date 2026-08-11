package com.verse.store.product.api.catalog.response;

import java.util.UUID;

public record CatalogProductVariantResponse(
        UUID id,
        String size,
        String colorName,
        String colorHex,
        int stockQuantity,
        boolean available) {
}
