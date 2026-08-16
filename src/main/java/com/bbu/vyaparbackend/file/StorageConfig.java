package com.bbu.vyaparbackend.file;

import io.minio.MinioClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(name = "app.storage.enabled", havingValue = "true", matchIfMissing = true)
public class StorageConfig {
    @Bean("minioClient")
    MinioClient minioClient(@Value("${app.storage.endpoint}") String endpoint, @Value("${app.storage.access-key}") String access, @Value("${app.storage.secret-key}") String secret) {
        return MinioClient.builder().endpoint(endpoint).credentials(access, secret).build();
    }

    @Bean("minioSigningClient")
    MinioClient minioSigningClient(@Value("${app.storage.public-endpoint:${app.storage.endpoint}}") String endpoint,
                                   @Value("${app.storage.access-key}") String access,
                                   @Value("${app.storage.secret-key}") String secret) {
        return MinioClient.builder().endpoint(endpoint).credentials(access, secret).build();
    }
}
