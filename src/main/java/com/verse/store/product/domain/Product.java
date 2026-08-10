package com.verse.store.product.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "products")
public class Product {

    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "slug", nullable = false, unique = true, length = 255)
    private String slug;

    @Column(name = "description", columnDefinition = "text")
    private String description;

    @Column(name = "brand", nullable = false, length = 255)
    private String brand;

    @Column(name = "collection_name", nullable = false, length = 255)
    private String collectionName;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 50)
    private ProductCategory category;

    @Column(name = "base_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal basePrice;

    @Column(name = "discount_percentage", nullable = false, precision = 5, scale = 2)
    private BigDecimal discountPercentage;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ProductStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @OneToMany(mappedBy = "product", fetch = FetchType.LAZY,
            cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProductVariant> variants = new ArrayList<>();

    @OneToMany(mappedBy = "product", fetch = FetchType.LAZY,
            cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProductImage> images = new ArrayList<>();

    protected Product() {
    }

    public Product(String name, String slug, String description, String brand,
            String collectionName, ProductCategory category, BigDecimal basePrice,
            BigDecimal discountPercentage) {
        this.name = name;
        this.slug = requireText(slug, "slug");
        this.description = description;
        this.brand = requireText(brand, "brand");
        this.collectionName = requireText(collectionName, "collectionName");
        this.category = Objects.requireNonNull(category, "category must not be null");
        setBasePrice(basePrice);
        setDiscountPercentage(discountPercentage);
        this.status = ProductStatus.DRAFT;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    public void setBasePrice(BigDecimal basePrice) {
        Objects.requireNonNull(basePrice, "basePrice must not be null");
        if (basePrice.signum() < 0) {
            throw new IllegalArgumentException("basePrice must not be negative");
        }
        this.basePrice = basePrice;
    }

    public void setDiscountPercentage(BigDecimal discountPercentage) {
        Objects.requireNonNull(discountPercentage, "discountPercentage must not be null");
        if (discountPercentage.signum() < 0 || discountPercentage.compareTo(ONE_HUNDRED) > 0) {
            throw new IllegalArgumentException("discountPercentage must be between 0 and 100");
        }
        this.discountPercentage = discountPercentage;
    }

    public void addVariant(ProductVariant variant) {
        Objects.requireNonNull(variant, "variant must not be null");
        if (variant.getProduct() != null && variant.getProduct() != this) {
            throw new IllegalArgumentException("variant already belongs to another product");
        }
        if (!variants.contains(variant)) {
            variants.add(variant);
            variant.attachTo(this);
        }
    }

    public void removeVariant(ProductVariant variant) {
        if (variants.remove(variant)) {
            variant.detachFrom(this);
        }
    }

    public void addImage(ProductImage image) {
        Objects.requireNonNull(image, "image must not be null");
        if (image.getProduct() != null && image.getProduct() != this) {
            throw new IllegalArgumentException("image already belongs to another product");
        }
        if (image.isPrimaryImage() && images.stream().anyMatch(ProductImage::isPrimaryImage)) {
            throw new IllegalArgumentException("product can have only one primary image");
        }
        if (!images.contains(image)) {
            images.add(image);
            image.attachTo(this);
        }
    }

    public void removeImage(ProductImage image) {
        if (images.remove(image)) {
            image.detachFrom(this);
        }
    }

    public void publish() {
        if (status != ProductStatus.DRAFT) {
            throw new IllegalStateException("only a DRAFT product can be published");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalStateException("product must have a name before publishing");
        }
        if (basePrice == null || basePrice.signum() < 0) {
            throw new IllegalStateException("product must have a valid price before publishing");
        }
        if (variants.isEmpty()) {
            throw new IllegalStateException("product must have at least one variant before publishing");
        }
        if (images.stream().noneMatch(ProductImage::isPrimaryImage)) {
            throw new IllegalStateException("product must have a primary image before publishing");
        }
        status = ProductStatus.ACTIVE;
    }

    public void archive() {
        status = ProductStatus.ARCHIVED;
    }

    public BigDecimal calculateFinalPrice() {
        BigDecimal multiplier = ONE_HUNDRED.subtract(discountPercentage).divide(ONE_HUNDRED);
        return basePrice.multiply(multiplier).setScale(2, RoundingMode.HALF_UP);
    }

    public int totalStock() {
        return variants.stream()
                .mapToInt(ProductVariant::getStockQuantity)
                .reduce(0, Math::addExact);
    }

    @PreUpdate
    void updateTimestamp() {
        updatedAt = Instant.now();
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getSlug() {
        return slug;
    }

    public String getDescription() {
        return description;
    }

    public String getBrand() {
        return brand;
    }

    public String getCollectionName() {
        return collectionName;
    }

    public ProductCategory getCategory() {
        return category;
    }

    public BigDecimal getBasePrice() {
        return basePrice;
    }

    public BigDecimal getDiscountPercentage() {
        return discountPercentage;
    }

    public ProductStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public List<ProductVariant> getVariants() {
        return Collections.unmodifiableList(variants);
    }

    public List<ProductImage> getImages() {
        return Collections.unmodifiableList(images);
    }
}
