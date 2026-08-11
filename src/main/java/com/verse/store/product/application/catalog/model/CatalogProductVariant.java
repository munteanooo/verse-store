package com.verse.store.product.application.catalog.model;

import java.util.UUID;

public record CatalogProductVariant(
        UUID id,
        String size,
        String colorName,
        String colorHex,
        int stockQuantity,
        boolean available) {
}
