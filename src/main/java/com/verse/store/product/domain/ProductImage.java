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
import jakarta.persistence.Table;

@Entity
@Table(name = "product_images")
public class ProductImage {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "url", nullable = false, columnDefinition = "text")
    private String url;

    @Column(name = "alt_text", length = 500)
    private String altText;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    @Column(name = "primary_image", nullable = false)
    private boolean primaryImage;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected ProductImage() {
    }

    public ProductImage(String url, String altText, int displayOrder, boolean primaryImage) {
        updateDetails(url, altText, displayOrder, primaryImage);
        this.createdAt = Instant.now();
    }

    public void updateDetails(String url, String altText, int displayOrder, boolean primaryImage) {
        this.url = requireText(url, "url");
        this.altText = altText;
        if (displayOrder < 0) {
            throw new IllegalArgumentException("displayOrder must not be negative");
        }
        this.displayOrder = displayOrder;
        this.primaryImage = primaryImage;
    }

    public void demoteFromPrimary() {
        this.primaryImage = false;
    }

    void attachTo(Product product) {
        this.product = Objects.requireNonNull(product);
    }

    void detachFrom(Product product) {
        if (this.product == product) {
            this.product = null;
        }
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

    public String getUrl() {
        return url;
    }

    public String getAltText() {
        return altText;
    }

    public int getDisplayOrder() {
        return displayOrder;
    }

    public boolean isPrimaryImage() {
        return primaryImage;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
