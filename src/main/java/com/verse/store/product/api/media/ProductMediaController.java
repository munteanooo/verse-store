package com.verse.store.product.api.media;

import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import com.verse.store.product.application.image.ProductImageStorage;

@RestController
public class ProductMediaController {
    private final ProductImageStorage storage;

    public ProductMediaController(ProductImageStorage storage) {
        this.storage = storage;
    }

    @GetMapping("/media/products/{filename:[a-f0-9\\-]+\\.(?:jpg|png|webp)}")
    public ResponseEntity<byte[]> image(@PathVariable String filename) {
        var image = storage.load("products/" + filename);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(image.contentType()))
                .cacheControl(CacheControl.maxAge(java.time.Duration.ofDays(30)).cachePublic())
                .body(image.content());
    }
}
