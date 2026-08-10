package com.verse.store.product.application.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.verse.store.product.application.command.CreateProductCommand;
import com.verse.store.product.application.command.CreateProductImageCommand;
import com.verse.store.product.application.command.CreateProductVariantCommand;
import com.verse.store.product.application.command.UpdateProductCommand;
import com.verse.store.product.application.exception.DuplicateProductException;
import com.verse.store.product.application.exception.InvalidProductStateException;
import com.verse.store.product.application.exception.ProductNotFoundException;
import com.verse.store.product.application.exception.ProductValidationException;
import com.verse.store.product.application.mapper.ProductMapper;
import com.verse.store.product.application.model.ProductResult;
import com.verse.store.product.application.query.ProductAdminQuery;
import com.verse.store.product.application.query.ProductSortDirection;
import com.verse.store.product.domain.Product;
import com.verse.store.product.domain.ProductImage;
import com.verse.store.product.domain.ProductStatus;
import com.verse.store.product.domain.ProductVariant;
import com.verse.store.product.infrastructure.persistence.ProductRepository;

@Service
public class ProductApplicationService {

    private static final Map<String, String> ALLOWED_SORTS = Map.of(
            "id", "id",
            "name", "name",
            "createdat", "createdAt",
            "updatedat", "updatedAt",
            "status", "status",
            "category", "category",
            "baseprice", "basePrice",
            "collectionname", "collectionName");

    private final ProductRepository productRepository;
    private final ProductMapper productMapper;
    private final ProductCommandValidator commandValidator;
    private final ProductSlugGenerator slugGenerator;

    public ProductApplicationService(
            ProductRepository productRepository,
            ProductMapper productMapper,
            ProductCommandValidator commandValidator,
            ProductSlugGenerator slugGenerator) {
        this.productRepository = productRepository;
        this.productMapper = productMapper;
        this.commandValidator = commandValidator;
        this.slugGenerator = slugGenerator;
    }

    @Transactional
    public ProductResult createProduct(CreateProductCommand command) {
        commandValidator.validate(command);
        Product product = new Product(
                command.name(),
                slugGenerator.uniqueSlug(command.name()),
                command.description(),
                command.brand(),
                command.collectionName(),
                command.category(),
                command.basePrice(),
                command.discountPercentage());
        addChildren(product, command.variants(), command.images());
        return productMapper.toResult(save(product));
    }

    @Transactional
    public ProductResult updateProduct(UpdateProductCommand command) {
        commandValidator.validate(command);
        Product product = findProduct(command.id());
        product.updateDetails(
                command.name(),
                command.description(),
                command.brand(),
                command.collectionName(),
                command.category(),
                command.basePrice(),
                command.discountPercentage());
        new ArrayList<>(product.getVariants()).forEach(product::removeVariant);
        new ArrayList<>(product.getImages()).forEach(product::removeImage);
        addChildren(product, command.variants(), command.images());
        return productMapper.toResult(save(product));
    }

    @Transactional
    public ProductResult publishProduct(UUID id) {
        Product product = findProduct(id);
        try {
            product.publish();
        } catch (IllegalStateException exception) {
            throw new InvalidProductStateException(exception.getMessage(), exception);
        }
        return productMapper.toResult(save(product));
    }

    @Transactional
    public ProductResult archiveProduct(UUID id) {
        Product product = findProduct(id);
        product.archive();
        return productMapper.toResult(save(product));
    }

    @Transactional
    public void deleteProduct(UUID id) {
        Product product = findProduct(id);
        if (product.getStatus() != ProductStatus.DRAFT) {
            throw new InvalidProductStateException(
                    "Only DRAFT products can be deleted; archive this product instead");
        }
        productRepository.delete(product);
    }

    @Transactional(readOnly = true)
    public ProductResult getProductForAdmin(UUID id) {
        return productMapper.toResult(findProduct(id));
    }

    @Transactional(readOnly = true)
    public Page<ProductResult> listProductsForAdmin(ProductAdminQuery query) {
        commandValidator.validate(query);
        String property = ALLOWED_SORTS.get(query.sortBy().toLowerCase(Locale.ROOT));
        if (property == null) {
            throw new ProductValidationException(List.of("unsupported sort field: " + query.sortBy()));
        }
        Sort.Direction direction = query.direction() == ProductSortDirection.ASC
                ? Sort.Direction.ASC : Sort.Direction.DESC;
        PageRequest pageRequest = PageRequest.of(
                query.page(), query.size(), Sort.by(direction, property));

        Page<Product> products;
        if (query.status() != null && query.category() != null) {
            products = productRepository.findByStatusAndCategory(
                    query.status(), query.category(), pageRequest);
        } else if (query.status() != null) {
            products = productRepository.findByStatus(query.status(), pageRequest);
        } else if (query.category() != null) {
            products = productRepository.findByCategory(query.category(), pageRequest);
        } else {
            products = productRepository.findAll(pageRequest);
        }
        return products.map(productMapper::toResult);
    }

    private Product findProduct(UUID id) {
        return productRepository.findById(id).orElseThrow(() -> new ProductNotFoundException(id));
    }

    private Product save(Product product) {
        try {
            return productRepository.saveAndFlush(product);
        } catch (DataIntegrityViolationException exception) {
            throw new DuplicateProductException(
                    "Product slug, SKU, or size/color combination already exists", exception);
        }
    }

    private void addChildren(
            Product product,
            List<CreateProductVariantCommand> variants,
            List<CreateProductImageCommand> images) {
        variants.forEach(variant -> product.addVariant(toVariant(variant)));
        images.forEach(image -> product.addImage(toImage(image)));
    }

    private ProductVariant toVariant(CreateProductVariantCommand command) {
        return new ProductVariant(
                command.sku(), command.size(), command.colorName(),
                command.colorHex(), command.stockQuantity());
    }

    private ProductImage toImage(CreateProductImageCommand command) {
        return new ProductImage(
                command.url(), command.altText(), command.displayOrder(), command.primaryImage());
    }
}
