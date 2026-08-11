package com.verse.store.product.api.catalog.response;

import java.util.UUID;

public record CatalogProductImageResponse(
        UUID id,
        String url,
        String altText,
        int displayOrder,
        boolean primaryImage) {
}
