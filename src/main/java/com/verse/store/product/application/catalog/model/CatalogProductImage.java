package com.verse.store.product.application.catalog.model;

import java.util.UUID;

public record CatalogProductImage(
        UUID id,
        String url,
        String altText,
        int displayOrder,
        boolean primaryImage) {
}
