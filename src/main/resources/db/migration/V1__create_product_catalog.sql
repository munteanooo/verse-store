CREATE TABLE products (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    slug VARCHAR(255) NOT NULL UNIQUE,
    description TEXT,
    brand VARCHAR(255) NOT NULL,
    collection_name VARCHAR(255) NOT NULL,
    category VARCHAR(50) NOT NULL,
    base_price NUMERIC(12, 2) NOT NULL,
    discount_percentage NUMERIC(5, 2) NOT NULL DEFAULT 0,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT chk_products_base_price_non_negative CHECK (base_price >= 0),
    CONSTRAINT chk_products_discount_percentage_range
        CHECK (discount_percentage BETWEEN 0 AND 100),
    CONSTRAINT chk_products_status
        CHECK (status IN ('DRAFT', 'ACTIVE', 'ARCHIVED')),
    CONSTRAINT chk_products_category
        CHECK (category IN ('TOPS', 'BOTTOMS', 'DRESSES', 'OUTERWEAR', 'KNITWEAR', 'ACCESSORIES'))
);

CREATE TABLE product_variants (
    id UUID PRIMARY KEY,
    product_id UUID NOT NULL,
    sku VARCHAR(100) NOT NULL UNIQUE,
    size VARCHAR(50) NOT NULL,
    color_name VARCHAR(100) NOT NULL,
    color_hex VARCHAR(20),
    stock_quantity INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_product_variants_product
        FOREIGN KEY (product_id) REFERENCES products (id) ON DELETE CASCADE,
    CONSTRAINT uq_product_variants_product_size_color
        UNIQUE (product_id, size, color_name),
    CONSTRAINT chk_product_variants_stock_non_negative CHECK (stock_quantity >= 0)
);

CREATE TABLE product_images (
    id UUID PRIMARY KEY,
    product_id UUID NOT NULL,
    url TEXT NOT NULL,
    alt_text VARCHAR(500),
    display_order INTEGER NOT NULL DEFAULT 0,
    primary_image BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_product_images_product
        FOREIGN KEY (product_id) REFERENCES products (id) ON DELETE CASCADE,
    CONSTRAINT chk_product_images_display_order_non_negative CHECK (display_order >= 0)
);

CREATE INDEX idx_products_status ON products (status);
CREATE INDEX idx_products_category ON products (category);
CREATE INDEX idx_products_collection_name ON products (collection_name);
CREATE INDEX idx_product_variants_product_id ON product_variants (product_id);
CREATE INDEX idx_product_images_product_id ON product_images (product_id);

CREATE UNIQUE INDEX uq_product_images_one_primary_per_product
    ON product_images (product_id)
    WHERE primary_image = TRUE;
