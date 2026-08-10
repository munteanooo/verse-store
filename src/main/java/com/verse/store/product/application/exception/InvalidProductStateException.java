package com.verse.store.product.application.exception;

public class InvalidProductStateException extends RuntimeException {

    public InvalidProductStateException(String message) {
        super(message);
    }

    public InvalidProductStateException(String message, Throwable cause) {
        super(message, cause);
    }
}
