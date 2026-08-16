package com.bbu.vyaparbackend.file;

import java.time.Instant;

public interface ObjectStorage {
    void store(String key, byte[] content, String contentType);

    SignedObject sign(String key);

    void delete(String key);

    record SignedObject(String key, String downloadUrl, Instant expiresAt) {
    }
}
