package com.verse.store.product.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "product_variants")
public class ProductVariant {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "sku", nullable = false, unique = true, length = 100)
    private String sku;

    @Column(name = "size", nullable = false, length = 50)
    private String size;

    @Column(name = "color_name", nullable = false, length = 100)
    private String colorName;

    @Column(name = "color_hex", length = 20)
    private String colorHex;

    @Column(name = "stock_quantity", nullable = false)
    private int stockQuantity;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected ProductVariant() {
    }

    public ProductVariant(String sku, String size, String colorName, String colorHex, int stockQuantity) {
        this.sku = requireText(sku, "sku");
        this.size = requireText(size, "size");
        this.colorName = requireText(colorName, "colorName");
        this.colorHex = colorHex;
        setStockQuantity(stockQuantity);
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    public void setStockQuantity(int stockQuantity) {
        if (stockQuantity < 0) {
            throw new IllegalArgumentException("stockQuantity must not be negative");
        }
        this.stockQuantity = stockQuantity;
    }

    public void updateDetails(
            String sku, String size, String colorName, String colorHex, int stockQuantity) {
        this.sku = requireText(sku, "sku");
        this.size = requireText(size, "size");
        this.colorName = requireText(colorName, "colorName");
        this.colorHex = colorHex;
        setStockQuantity(stockQuantity);
    }

    void attachTo(Product product) {
        this.product = Objects.requireNonNull(product);
    }

    void detachFrom(Product product) {
        if (this.product == product) {
            this.product = null;
        }
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

    public Product getProduct() {
        return product;
    }

    public String getSku() {
        return sku;
    }

    public String getSize() {
        return size;
    }

    public String getColorName() {
        return colorName;
    }

    public String getColorHex() {
        return colorHex;
    }

    public int getStockQuantity() {
        return stockQuantity;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
