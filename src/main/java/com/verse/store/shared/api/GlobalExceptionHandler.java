package com.verse.store.shared.api;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

import com.verse.store.product.application.exception.DuplicateProductException;
import com.verse.store.product.application.exception.InvalidProductStateException;
import com.verse.store.product.application.exception.ProductNotFoundException;
import com.verse.store.product.application.exception.ProductValidationException;
import com.verse.store.product.application.image.ProductImageStorageException;
import com.verse.store.product.application.image.ProductImageUploadException;
import com.verse.store.product.application.catalog.exception.CatalogProductNotFoundException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ValidationErrorResponse> handleInvalidBody(
            MethodArgumentNotValidException exception, HttpServletRequest request) {
        Map<String, List<String>> fieldErrors = new LinkedHashMap<>();
        for (FieldError error : exception.getBindingResult().getFieldErrors()) {
            fieldErrors.computeIfAbsent(error.getField(), ignored -> new java.util.ArrayList<>())
                    .add(error.getDefaultMessage());
        }
        return validationError(
                HttpStatus.BAD_REQUEST, "REQUEST_VALIDATION_FAILED",
                "Request validation failed", request, fieldErrors);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    ResponseEntity<ValidationErrorResponse> handleConstraintViolation(
            ConstraintViolationException exception, HttpServletRequest request) {
        Map<String, List<String>> fieldErrors = new LinkedHashMap<>();
        for (ConstraintViolation<?> violation : exception.getConstraintViolations()) {
            String field = violation.getPropertyPath().toString();
            fieldErrors.computeIfAbsent(field, ignored -> new java.util.ArrayList<>())
                    .add(violation.getMessage());
        }
        return validationError(
                HttpStatus.BAD_REQUEST, "REQUEST_PARAMETER_INVALID",
                "Request parameters are invalid", request, fieldErrors);
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    ResponseEntity<ValidationErrorResponse> handleMethodValidation(
            HandlerMethodValidationException exception, HttpServletRequest request) {
        return validationError(
                HttpStatus.BAD_REQUEST, "REQUEST_PARAMETER_INVALID",
                "Request parameters are invalid", request,
                Map.of("request", List.of("One or more request parameters are outside the allowed range")));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    ResponseEntity<ValidationErrorResponse> handleTypeMismatch(
            MethodArgumentTypeMismatchException exception, HttpServletRequest request) {
        return validationError(
                HttpStatus.BAD_REQUEST, "REQUEST_PARAMETER_INVALID",
                "Request parameter has an invalid value", request,
                Map.of(exception.getName(), List.of("invalid value")));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ApiErrorResponse> handleUnreadableBody(
            HttpMessageNotReadableException exception, HttpServletRequest request) {
        return error(HttpStatus.BAD_REQUEST, "MALFORMED_JSON", "Request body is invalid", request);
    }

    @ExceptionHandler(ProductImageUploadException.class)
    ResponseEntity<ApiErrorResponse> handleImageUpload(
            ProductImageUploadException exception, HttpServletRequest request) {
        return error(exception.getStatus(), exception.getCode(), exception.getMessage(), request);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    ResponseEntity<ApiErrorResponse> handleMaxUpload(
            MaxUploadSizeExceededException exception, HttpServletRequest request) {
        return error(HttpStatus.PAYLOAD_TOO_LARGE, "IMAGE_TOO_LARGE",
                "The image exceeds the upload limit", request);
    }

    @ExceptionHandler(MissingServletRequestPartException.class)
    ResponseEntity<ApiErrorResponse> handleMissingMultipartPart(
            MissingServletRequestPartException exception, HttpServletRequest request) {
        return error(HttpStatus.BAD_REQUEST, "INVALID_MULTIPART_REQUEST",
                "A product image file is required", request);
    }

    @ExceptionHandler(ProductImageStorageException.class)
    ResponseEntity<ApiErrorResponse> handleStorageUnavailable(
            ProductImageStorageException exception, HttpServletRequest request) {
        return error(HttpStatus.SERVICE_UNAVAILABLE, "IMAGE_STORAGE_UNAVAILABLE",
                "Product image storage is temporarily unavailable", request);
    }

    @ExceptionHandler(ProductNotFoundException.class)
    ResponseEntity<ApiErrorResponse> handleNotFound(
            ProductNotFoundException exception, HttpServletRequest request) {
        return error(HttpStatus.NOT_FOUND, "PRODUCT_NOT_FOUND", exception.getMessage(), request);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    ResponseEntity<ApiErrorResponse> handleMissingResource(
            NoResourceFoundException exception, HttpServletRequest request) {
        return error(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", "Resource not found", request);
    }

    @ExceptionHandler(CatalogProductNotFoundException.class)
    ResponseEntity<ApiErrorResponse> handleCatalogNotFound(
            CatalogProductNotFoundException exception, HttpServletRequest request) {
        return error(
                HttpStatus.NOT_FOUND, "CATALOG_PRODUCT_NOT_FOUND",
                "Catalog product not found", request);
    }

    @ExceptionHandler(DuplicateProductException.class)
    ResponseEntity<ApiErrorResponse> handleDuplicate(
            DuplicateProductException exception, HttpServletRequest request) {
        String message = switch (exception.getMessage()) {
            case "A product with this slug already exists",
                    "A product variant with this SKU already exists",
                    "This product already has the same size and color combination",
                    "A product can have at most one primary image" -> exception.getMessage();
            default -> "A product with the same slug, SKU, or variant option already exists";
        };
        return error(
                HttpStatus.CONFLICT, "DUPLICATE_PRODUCT",
                message, request);
    }

    @ExceptionHandler(InvalidProductStateException.class)
    ResponseEntity<ApiErrorResponse> handleInvalidState(
            InvalidProductStateException exception, HttpServletRequest request) {
        return error(HttpStatus.CONFLICT, "INVALID_PRODUCT_STATE", exception.getMessage(), request);
    }

    @ExceptionHandler(ProductValidationException.class)
    ResponseEntity<ValidationErrorResponse> handleProductValidation(
            ProductValidationException exception, HttpServletRequest request) {
        return validationError(
                HttpStatus.UNPROCESSABLE_CONTENT, "PRODUCT_VALIDATION_FAILED",
                "Product validation failed", request,
                Map.of("product", exception.getViolations()));
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiErrorResponse> handleUnexpected(
            Exception exception, HttpServletRequest request) {
        LOGGER.error("Unexpected error while processing {}", request.getRequestURI(), exception);
        return error(
                HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR",
                "An unexpected server error occurred", request);
    }

    private ResponseEntity<ApiErrorResponse> error(
            HttpStatus status, String code, String message, HttpServletRequest request) {
        return ResponseEntity.status(status).body(new ApiErrorResponse(
                Instant.now(), status.value(), status.getReasonPhrase(), code,
                message, request.getRequestURI()));
    }

    private ResponseEntity<ValidationErrorResponse> validationError(
            HttpStatus status,
            String code,
            String message,
            HttpServletRequest request,
            Map<String, List<String>> fieldErrors) {
        return ResponseEntity.status(status).body(new ValidationErrorResponse(
                Instant.now(), status.value(), status.getReasonPhrase(), code,
                message, request.getRequestURI(), Map.copyOf(fieldErrors)));
    }
}
