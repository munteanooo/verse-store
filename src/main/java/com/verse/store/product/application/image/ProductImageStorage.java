package com.verse.store.product.application.image;

public interface ProductImageStorage {

    void store(String objectKey, byte[] content, String contentType);

    StoredImage load(String objectKey);

    record StoredImage(byte[] content, String contentType) {
        public StoredImage {
            content = content.clone();
        }

        @Override
        public byte[] content() {
            return content.clone();
        }
    }
}
