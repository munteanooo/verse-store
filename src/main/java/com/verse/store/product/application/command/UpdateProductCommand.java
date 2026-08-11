package com.verse.store.product.application.command;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import com.verse.store.product.domain.ProductCategory;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateProductCommand(
        @NotNull UUID id,
        @NotBlank @Size(max = 255) String name,
        String description,
        @NotBlank @Size(max = 255) String brand,
        @NotBlank @Size(max = 255) String collectionName,
        @NotNull ProductCategory category,
        @NotNull @DecimalMin("0.00") BigDecimal basePrice,
        @NotNull @DecimalMin("0.00") @DecimalMax("100.00") BigDecimal discountPercentage,
        @NotNull List<@Valid UpdateProductVariantCommand> variants,
        @NotNull List<@Valid UpdateProductImageCommand> images) {

    public UpdateProductCommand {
        variants = variants == null ? null : List.copyOf(variants);
        images = images == null ? null : List.copyOf(images);
    }
}
