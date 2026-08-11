package com.verse.store.product.application.catalog.exception;

public class CatalogProductNotFoundException extends RuntimeException {

    public CatalogProductNotFoundException() {
        super("Catalog product not found");
    }
}
