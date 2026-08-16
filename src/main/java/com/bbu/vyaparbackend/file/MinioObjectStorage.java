package com.bbu.vyaparbackend.file;

import com.bbu.vyaparbackend.shared.ApiException;
import com.bbu.vyaparbackend.shared.ErrorMessages;
import io.minio.*;
import io.minio.http.Method;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.time.Instant;

@Service
@ConditionalOnProperty(name = "app.storage.enabled", havingValue = "true", matchIfMissing = true)
public class MinioObjectStorage implements ObjectStorage {
    private final MinioClient client;
    private final MinioClient signingClient;
    private final String bucket;
    private final int signedUrlTtlSeconds;

    public MinioObjectStorage(@Qualifier("minioClient") MinioClient client,
                              @Qualifier("minioSigningClient") MinioClient signingClient,
                              @Value("${app.storage.bucket}") String bucket,
                              @Value("${app.storage.signed-url-ttl-seconds:900}") int signedUrlTtlSeconds) {
        this.client = client;
        this.signingClient = signingClient;
        this.bucket = bucket;
        this.signedUrlTtlSeconds = signedUrlTtlSeconds;
    }

    @PostConstruct
    void initialize() {
        try {
            if (!client.bucketExists(BucketExistsArgs.builder().bucket(bucket).build())) {
                client.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
            }
            client.deleteBucketPolicy(DeleteBucketPolicyArgs.builder().bucket(bucket).build());
        } catch (io.minio.errors.ErrorResponseException exception) {
            if (!"NoSuchBucketPolicy".equals(exception.errorResponse().code())) {
                throw new IllegalStateException("Object storage is unavailable", exception);
            }
        } catch (Exception exception) {
            throw new IllegalStateException("Object storage is unavailable", exception);
        }
    }

    @Override
    public void store(String key, byte[] content, String contentType) {
        try {
            client.putObject(PutObjectArgs.builder().bucket(bucket).object(key)
                    .stream(new ByteArrayInputStream(content), content.length, -1)
                    .contentType(contentType).build());
        } catch (Exception exception) {
            throw unavailable("Could not store the file", exception);
        }
    }

    @Override
    public SignedObject sign(String key) {
        try {
            String url = signingClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder().bucket(bucket).object(key)
                    .method(Method.GET).expiry(signedUrlTtlSeconds).build());
            return new SignedObject(key, url, Instant.now().plusSeconds(signedUrlTtlSeconds));
        } catch (Exception exception) {
            throw unavailable("Could not create a file download URL", exception);
        }
    }

    @Override
    public void delete(String key) {
        try {
            client.removeObject(RemoveObjectArgs.builder().bucket(bucket).object(key).build());
        } catch (Exception exception) {
            throw unavailable("Could not remove the file", exception);
        }
    }

    private ApiException unavailable(String message, Exception cause) {
        ApiException exception = new ApiException(org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE,
                ErrorMessages.Codes.STORAGE_UNAVAILABLE, message);
        exception.initCause(cause);
        return exception;
    }
}
