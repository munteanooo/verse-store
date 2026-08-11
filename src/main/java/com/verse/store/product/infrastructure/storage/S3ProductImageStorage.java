package com.verse.store.product.infrastructure.storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.verse.store.product.application.image.ProductImageStorage;
import com.verse.store.product.application.image.ProductImageStorageException;

import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

@Component
public class S3ProductImageStorage implements ProductImageStorage {

    private final S3Client client;
    private final String bucket;

    public S3ProductImageStorage(S3Client client, @Value("${app.product-images.bucket}") String bucket) {
        this.client = client;
        this.bucket = bucket;
    }

    @Override
    public void store(String objectKey, byte[] content, String contentType) {
        try {
            ensureBucket();
            client.putObject(PutObjectRequest.builder().bucket(bucket).key(objectKey)
                    .contentType(contentType).contentLength((long) content.length).build(),
                    RequestBody.fromBytes(content));
        } catch (RuntimeException exception) {
            throw new ProductImageStorageException("Product image storage is unavailable", exception);
        }
    }

    @Override
    public StoredImage load(String objectKey) {
        try {
            ensureBucket();
            var response = client.getObjectAsBytes(GetObjectRequest.builder()
                    .bucket(bucket).key(objectKey).build());
            return new StoredImage(response.asByteArray(), response.response().contentType());
        } catch (RuntimeException exception) {
            throw new ProductImageStorageException("Product image storage is unavailable", exception);
        }
    }

    private void ensureBucket() {
        try {
            client.headBucket(HeadBucketRequest.builder().bucket(bucket).build());
        } catch (S3Exception exception) {
            if (exception.statusCode() != 404) {
                throw new ProductImageStorageException("Product image storage is unavailable", exception);
            }
            try {
                client.createBucket(CreateBucketRequest.builder().bucket(bucket).build());
            } catch (S3Exception createException) {
                if (createException.statusCode() != 409) {
                    throw new ProductImageStorageException("Product image storage is unavailable", createException);
                }
            }
        }
    }
}
