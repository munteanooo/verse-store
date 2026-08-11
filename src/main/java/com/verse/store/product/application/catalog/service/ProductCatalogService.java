package com.verse.store.product.application.catalog.service;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.verse.store.product.application.catalog.exception.CatalogProductNotFoundException;
import com.verse.store.product.application.catalog.mapper.ProductCatalogMapper;
import com.verse.store.product.application.catalog.model.CatalogFilterOption;
import com.verse.store.product.application.catalog.model.CatalogProductDetails;
import com.verse.store.product.application.catalog.model.CatalogProductSummary;
import com.verse.store.product.application.catalog.query.CatalogProductQuery;
import com.verse.store.product.domain.Product;
import com.verse.store.product.domain.ProductCategory;
import com.verse.store.product.infrastructure.persistence.ProductRepository;

@Service
public class ProductCatalogService {

    private static final Sort CATALOG_SORT = Sort.by(
            Sort.Order.desc("createdAt"), Sort.Order.asc("id"));

    private final ProductRepository productRepository;
    private final ProductCatalogMapper catalogMapper;
    private final CatalogQueryValidator queryValidator;

    public ProductCatalogService(
            ProductRepository productRepository,
            ProductCatalogMapper catalogMapper,
            CatalogQueryValidator queryValidator) {
        this.productRepository = productRepository;
        this.catalogMapper = catalogMapper;
        this.queryValidator = queryValidator;
    }

    @Transactional(readOnly = true)
    public Page<CatalogProductSummary> listActiveProducts(CatalogProductQuery query) {
        queryValidator.validate(query);
        PageRequest pageRequest = PageRequest.of(query.page(), query.size(), CATALOG_SORT);
        Page<Product> products = productRepository.findActiveCatalogProducts(
                query.category(), normalizeCollection(query.collection()), pageRequest);
        initializeCatalogCollections(products.getContent());
        return products.map(catalogMapper::toSummary);
    }

    @Transactional(readOnly = true)
    public CatalogProductDetails getActiveProductBySlug(String slug) {
        Product product = productRepository.findActiveCatalogProductBySlug(slug)
                .orElseThrow(CatalogProductNotFoundException::new);
        initializeCatalogCollections(List.of(product));
        return catalogMapper.toDetails(product);
    }

    @Transactional(readOnly = true)
    public List<CatalogFilterOption> listActiveCategories() {
        return Arrays.stream(ProductCategory.values())
                .map(category -> new CatalogFilterOption(category.name(), label(category)))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<String> listActiveCollections() {
        return productRepository.findDistinctActiveCollectionNames();
    }

    private void initializeCatalogCollections(List<Product> products) {
        if (products.isEmpty()) {
            return;
        }
        List<UUID> ids = products.stream().map(Product::getId).toList();
        productRepository.loadCatalogVariants(ids);
        productRepository.loadCatalogImages(ids);
    }

    private static String normalizeCollection(String collection) {
        return collection == null || collection.isBlank() ? null : collection.trim();
    }

    private static String label(ProductCategory category) {
        String lowerCase = category.name().toLowerCase(Locale.ROOT).replace('_', ' ');
        return Character.toUpperCase(lowerCase.charAt(0)) + lowerCase.substring(1);
    }
}
