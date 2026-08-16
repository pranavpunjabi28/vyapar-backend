package com.bbu.vyaparbackend.file;

import java.time.Instant;

public final class MediaApi {
    private MediaApi() {
    }

    public record FileResponse(String key, String downloadUrl, Instant expiresAt) {
        static FileResponse from(ObjectStorage.SignedObject object) {
            return new FileResponse(object.key(), object.downloadUrl(), object.expiresAt());
        }
    }
}
