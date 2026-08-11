package com.verse.store.product.api.admin.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record ProductVariantRequest(
        @NotBlank @Size(max = 100) String sku,
        @NotBlank @Size(max = 50) String size,
        @NotBlank @Size(max = 100) String colorName,
        @Size(max = 20) String colorHex,
        @PositiveOrZero int stockQuantity) {
}
