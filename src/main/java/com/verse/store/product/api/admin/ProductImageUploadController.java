package com.verse.store.product.api.admin;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.verse.store.product.application.image.ProductImageUploadResult;
import com.verse.store.product.application.image.ProductImageUploadService;

@RestController
public class ProductImageUploadController {
    private final ProductImageUploadService service;

    public ProductImageUploadController(ProductImageUploadService service) {
        this.service = service;
    }

    @PostMapping(path = "/api/admin/product-images", consumes = "multipart/form-data")
    @ResponseStatus(HttpStatus.CREATED)
    public ProductImageUploadResult upload(@RequestPart("file") MultipartFile file) {
        return service.upload(file);
    }
}
