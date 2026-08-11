package com.verse.store.product.api.admin;

import java.net.URI;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.verse.store.product.api.admin.mapper.AdminProductApiMapper;
import com.verse.store.product.api.admin.request.CreateProductRequest;
import com.verse.store.product.api.admin.request.UpdateProductRequest;
import com.verse.store.product.api.admin.response.AdminProductResponse;
import com.verse.store.product.application.model.ProductResult;
import com.verse.store.product.application.query.ProductAdminQuery;
import com.verse.store.product.application.query.ProductSortDirection;
import com.verse.store.product.application.service.ProductApplicationService;
import com.verse.store.product.domain.ProductCategory;
import com.verse.store.product.domain.ProductStatus;
import com.verse.store.shared.api.PagedResponse;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/**
 * Development-only administration API. It is intentionally unsecured for local
 * development until the dedicated security checkpoint is implemented.
 */
@Validated
@RestController
@RequestMapping("/api/admin/products")
public class AdminProductController {

    private final ProductApplicationService productService;
    private final AdminProductApiMapper apiMapper;

    public AdminProductController(
            ProductApplicationService productService,
            AdminProductApiMapper apiMapper) {
        this.productService = productService;
        this.apiMapper = apiMapper;
    }

    @PostMapping
    public ResponseEntity<AdminProductResponse> createProduct(
            @Valid @RequestBody CreateProductRequest request) {
        ProductResult result = productService.createProduct(apiMapper.toCommand(request));
        URI location = URI.create("/api/admin/products/" + result.id());
        return ResponseEntity.created(location).body(apiMapper.toResponse(result));
    }

    @GetMapping("/{id}")
    public AdminProductResponse getProduct(@PathVariable UUID id) {
        return apiMapper.toResponse(productService.getProductForAdmin(id));
    }

    @GetMapping
    public PagedResponse<AdminProductResponse> listProducts(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(required = false) ProductStatus status,
            @RequestParam(required = false) ProductCategory category,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") ProductSortDirection direction) {
        ProductAdminQuery query = new ProductAdminQuery(
                page, size, status, category, sortBy, direction);
        Page<AdminProductResponse> result = productService.listProductsForAdmin(query)
                .map(apiMapper::toResponse);
        return PagedResponse.from(result);
    }

    @PutMapping("/{id}")
    public AdminProductResponse updateProduct(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateProductRequest request) {
        return apiMapper.toResponse(productService.updateProduct(apiMapper.toCommand(id, request)));
    }

    @PatchMapping("/{id}/publish")
    public AdminProductResponse publishProduct(@PathVariable UUID id) {
        return apiMapper.toResponse(productService.publishProduct(id));
    }

    @PatchMapping("/{id}/archive")
    public AdminProductResponse archiveProduct(@PathVariable UUID id) {
        return apiMapper.toResponse(productService.archiveProduct(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable UUID id) {
        productService.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }
}
