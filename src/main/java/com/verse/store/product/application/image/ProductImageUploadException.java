package com.verse.store.product.application.image;

import org.springframework.http.HttpStatus;

public class ProductImageUploadException extends RuntimeException {

    private final HttpStatus status;
    private final String code;

    public ProductImageUploadException(HttpStatus status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getCode() {
        return code;
    }
}
