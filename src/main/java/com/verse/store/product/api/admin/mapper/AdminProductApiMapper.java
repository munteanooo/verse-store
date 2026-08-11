package com.verse.store.product.api.admin.mapper;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.verse.store.product.api.admin.request.CreateProductRequest;
import com.verse.store.product.api.admin.request.ProductImageRequest;
import com.verse.store.product.api.admin.request.ProductVariantRequest;
import com.verse.store.product.api.admin.request.UpdateProductRequest;
import com.verse.store.product.api.admin.response.AdminProductImageResponse;
import com.verse.store.product.api.admin.response.AdminProductResponse;
import com.verse.store.product.api.admin.response.AdminProductVariantResponse;
import com.verse.store.product.application.command.CreateProductCommand;
import com.verse.store.product.application.command.CreateProductImageCommand;
import com.verse.store.product.application.command.CreateProductVariantCommand;
import com.verse.store.product.application.command.UpdateProductCommand;
import com.verse.store.product.application.command.UpdateProductImageCommand;
import com.verse.store.product.application.command.UpdateProductVariantCommand;
import com.verse.store.product.application.exception.ProductValidationException;
import com.verse.store.product.application.model.ProductImageResult;
import com.verse.store.product.application.model.ProductResult;
import com.verse.store.product.application.model.ProductVariantResult;

@Component
public class AdminProductApiMapper {

    public CreateProductCommand toCommand(CreateProductRequest request) {
        if (request.variants().stream().anyMatch(item -> item.id() != null)
                || request.images().stream().anyMatch(item -> item.id() != null)) {
            throw new ProductValidationException(List.of("child IDs are not allowed when creating a product"));
        }
        return new CreateProductCommand(
                request.name(),
                request.description(),
                request.brand(),
                request.collectionName(),
                request.category(),
                request.basePrice(),
                request.discountPercentage(),
                request.variants().stream().map(this::toCreateCommand).toList(),
                request.images().stream().map(this::toCreateCommand).toList());
    }

    public UpdateProductCommand toCommand(UUID id, UpdateProductRequest request) {
        return new UpdateProductCommand(
                id,
                request.name(),
                request.description(),
                request.brand(),
                request.collectionName(),
                request.category(),
                request.basePrice(),
                request.discountPercentage(),
                request.variants().stream().map(this::toUpdateCommand).toList(),
                request.images().stream().map(this::toUpdateCommand).toList());
    }

    public AdminProductResponse toResponse(ProductResult result) {
        return new AdminProductResponse(
                result.id(),
                result.name(),
                result.slug(),
                result.description(),
                result.brand(),
                result.collectionName(),
                result.category(),
                result.basePrice(),
                result.discountPercentage(),
                result.finalPrice(),
                result.status(),
                result.totalStock(),
                result.variants().stream().map(this::toResponse).toList(),
                result.images().stream().map(this::toResponse).toList(),
                result.createdAt(),
                result.updatedAt());
    }

    private CreateProductVariantCommand toCreateCommand(ProductVariantRequest request) {
        return new CreateProductVariantCommand(
                request.sku(), request.size(), request.colorName(),
                request.colorHex(), request.stockQuantity());
    }

    private CreateProductImageCommand toCreateCommand(ProductImageRequest request) {
        return new CreateProductImageCommand(
                request.url(), request.altText(), request.displayOrder(), request.primaryImage());
    }

    private UpdateProductVariantCommand toUpdateCommand(ProductVariantRequest request) {
        return new UpdateProductVariantCommand(
                request.id(), request.sku(), request.size(), request.colorName(),
                request.colorHex(), request.stockQuantity());
    }

    private UpdateProductImageCommand toUpdateCommand(ProductImageRequest request) {
        return new UpdateProductImageCommand(
                request.id(), request.url(), request.altText(), request.displayOrder(), request.primaryImage());
    }

    private AdminProductVariantResponse toResponse(ProductVariantResult result) {
        return new AdminProductVariantResponse(
                result.id(), result.sku(), result.size(), result.colorName(),
                result.colorHex(), result.stockQuantity(), result.createdAt(), result.updatedAt());
    }

    private AdminProductImageResponse toResponse(ProductImageResult result) {
        return new AdminProductImageResponse(
                result.id(), result.url(), result.altText(), result.displayOrder(),
                result.primaryImage(), result.createdAt());
    }
}
