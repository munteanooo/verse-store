package com.verse.store.product.api.catalog;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.verse.store.product.api.catalog.mapper.CatalogProductApiMapper;
import com.verse.store.product.api.catalog.response.CatalogFilterOptionResponse;
import com.verse.store.product.api.catalog.response.CatalogProductDetailsResponse;
import com.verse.store.product.api.catalog.response.CatalogProductSummaryResponse;
import com.verse.store.product.application.catalog.model.CatalogProductSummary;
import com.verse.store.product.application.catalog.query.CatalogProductQuery;
import com.verse.store.product.application.catalog.service.ProductCatalogService;
import com.verse.store.product.domain.ProductCategory;
import com.verse.store.shared.api.PagedResponse;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

@Validated
@RestController
@RequestMapping("/api/catalog")
public class CatalogProductController {

    private final ProductCatalogService catalogService;
    private final CatalogProductApiMapper apiMapper;

    public CatalogProductController(
            ProductCatalogService catalogService,
            CatalogProductApiMapper apiMapper) {
        this.catalogService = catalogService;
        this.apiMapper = apiMapper;
    }

    @GetMapping("/products")
    public PagedResponse<CatalogProductSummaryResponse> listProducts(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(required = false) ProductCategory category,
            @RequestParam(required = false) String collection) {
        Page<CatalogProductSummary> products = catalogService.listActiveProducts(
                new CatalogProductQuery(page, size, category, collection));
        return PagedResponse.from(products.map(apiMapper::toResponse));
    }

    @GetMapping("/products/{slug}")
    public CatalogProductDetailsResponse getProduct(@PathVariable String slug) {
        return apiMapper.toResponse(catalogService.getActiveProductBySlug(slug));
    }

    @GetMapping("/categories")
    public List<CatalogFilterOptionResponse> listCategories() {
        return catalogService.listActiveCategories().stream().map(apiMapper::toResponse).toList();
    }

    @GetMapping("/collections")
    public List<String> listCollections() {
        return catalogService.listActiveCollections();
    }
}
