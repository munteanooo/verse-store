package com.verse.store.product.application.command;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record UpdateProductVariantCommand(
        UUID id,
        @NotBlank @Size(max = 100) String sku,
        @NotBlank @Size(max = 50) String size,
        @NotBlank @Size(max = 100) String colorName,
        @Size(max = 20) String colorHex,
        @PositiveOrZero int stockQuantity) {
}
