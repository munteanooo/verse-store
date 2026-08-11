package com.verse.store.product.infrastructure.storage;

import java.net.URI;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

@Configuration
class ProductImageStorageConfiguration {
    @Bean(destroyMethod = "close")
    S3Client productImageS3Client(
            @Value("${app.product-images.endpoint}") URI endpoint,
            @Value("${app.product-images.access-key}") String accessKey,
            @Value("${app.product-images.secret-key}") String secretKey,
            @Value("${app.product-images.region:us-east-1}") String region) {
        return S3Client.builder().endpointOverride(endpoint).region(Region.of(region))
                .forcePathStyle(true)
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKey, secretKey)))
                .build();
    }
}
