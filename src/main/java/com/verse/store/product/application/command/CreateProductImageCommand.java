package com.verse.store.product.application.command;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record CreateProductImageCommand(
        @NotBlank String url,
        @Size(max = 500) String altText,
        @PositiveOrZero int displayOrder,
        boolean primaryImage) {
}
