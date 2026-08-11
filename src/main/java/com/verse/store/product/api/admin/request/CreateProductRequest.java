package com.verse.store.product.api.admin.request;

import java.math.BigDecimal;
import java.util.List;

import com.verse.store.product.domain.ProductCategory;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateProductRequest(
        @NotBlank @Size(max = 255) String name,
        String description,
        @NotBlank @Size(max = 255) String brand,
        @NotBlank @Size(max = 255) String collectionName,
        @NotNull ProductCategory category,
        @NotNull @DecimalMin("0.00") BigDecimal basePrice,
        @NotNull @DecimalMin("0.00") @DecimalMax("100.00") BigDecimal discountPercentage,
        @NotNull List<@Valid ProductVariantRequest> variants,
        @NotNull List<@Valid ProductImageRequest> images) {

    public CreateProductRequest {
        variants = variants == null ? null : List.copyOf(variants);
        images = images == null ? null : List.copyOf(images);
    }
}
