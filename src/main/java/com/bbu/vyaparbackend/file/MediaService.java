package com.bbu.vyaparbackend.file;

import com.bbu.vyaparbackend.business.BusinessManagementService;
import com.bbu.vyaparbackend.business.Membership;
import com.bbu.vyaparbackend.business.Outlet;
import com.bbu.vyaparbackend.catalog.CatalogManagementService;
import com.bbu.vyaparbackend.catalog.CatalogQueryService;
import com.bbu.vyaparbackend.catalog.Product;
import com.bbu.vyaparbackend.shared.ApiException;
import com.bbu.vyaparbackend.shared.PrefixedIdGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Set;

@Service
@ConditionalOnProperty(name = "app.storage.enabled", havingValue = "true", matchIfMissing = true)
public class MediaService {
    private static final Logger log = LoggerFactory.getLogger(MediaService.class);
    private static final Set<String> ALLOWED_TYPES = Set.of("image/jpeg", "image/png", "image/webp");
    private final ObjectStorage storage;
    private final BusinessManagementService businesses;
    private final CatalogQueryService catalog;
    private final CatalogManagementService catalogManagement;
    private final long maxFileSize;

    public MediaService(ObjectStorage storage, BusinessManagementService businesses, CatalogQueryService catalog,
                        CatalogManagementService catalogManagement,
                        @Value("${app.storage.max-file-size-bytes:5242880}") long maxFileSize) {
        this.storage = storage;
        this.businesses = businesses;
        this.catalog = catalog;
        this.catalogManagement = catalogManagement;
        this.maxFileSize = maxFileSize;
    }

    @Transactional
    public MediaApi.FileResponse replaceBusinessLogo(Membership actor, MultipartFile file) {
        ValidatedUpload upload = validate(file);
        String oldKey = actor.getBusiness().getLogoKey();
        String key = "businesses/" + actor.getBusiness().getId() + "/logo/" + objectName(upload.extension());
        storeAndRegisterCleanup(key, oldKey, upload);
        businesses.setLogo(actor, key);
        return MediaApi.FileResponse.from(storage.sign(key));
    }

    @Transactional(readOnly = true)
    public MediaApi.FileResponse businessLogo(Membership actor) {
        return signed(actor.getBusiness().getLogoKey(), "Business logo");
    }

    @Transactional
    public MediaApi.FileResponse replaceProductImage(Outlet outlet, String productId, MultipartFile file) {
        ValidatedUpload upload = validate(file);
        Product product = catalog.product(outlet, productId);
        String oldKey = product.getImageKey();
        String key = "businesses/" + outlet.getBusiness().getId() + "/products/" + objectName(upload.extension());
        storeAndRegisterCleanup(key, oldKey, upload);
        catalogManagement.setProductImage(outlet, productId, key);
        return MediaApi.FileResponse.from(storage.sign(key));
    }

    @Transactional(readOnly = true)
    public MediaApi.FileResponse productImage(Outlet outlet, String productId) {
        return signed(catalog.product(outlet, productId).getImageKey(), "Product image");
    }

    private MediaApi.FileResponse signed(String key, String resource) {
        if (key == null || key.isBlank()) throw ApiException.notFound(resource);
        return MediaApi.FileResponse.from(storage.sign(key));
    }

    private void storeAndRegisterCleanup(String newKey, String oldKey, ValidatedUpload upload) {
        storage.store(newKey, upload.content(), upload.contentType());
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                if (oldKey != null && !oldKey.isBlank() && !oldKey.equals(newKey)) safeDelete(oldKey);
            }

            @Override
            public void afterCompletion(int status) {
                if (status != STATUS_COMMITTED) safeDelete(newKey);
            }
        });
    }

    private ValidatedUpload validate(MultipartFile file) {
        String contentType = file.getContentType();
        if (file.isEmpty() || file.getSize() > maxFileSize || !ALLOWED_TYPES.contains(contentType)) {
            throw ApiException.invalid("Only JPEG, PNG, and WebP images up to 5 MB are supported");
        }
        try {
            byte[] content = file.getBytes();
            if (!matchesSignature(contentType, content)) {
                throw ApiException.invalid("The uploaded file content does not match its image type");
            }
            return new ValidatedUpload(content, contentType, extension(contentType));
        } catch (IOException exception) {
            throw ApiException.invalid("The uploaded file could not be read");
        }
    }

    private boolean matchesSignature(String contentType, byte[] value) {
        return switch (contentType) {
            case "image/jpeg" -> value.length >= 3 && unsigned(value[0]) == 0xff && unsigned(value[1]) == 0xd8
                    && unsigned(value[2]) == 0xff;
            case "image/png" -> value.length >= 8 && unsigned(value[0]) == 0x89 && value[1] == 'P'
                    && value[2] == 'N' && value[3] == 'G';
            case "image/webp" -> value.length >= 12 && value[0] == 'R' && value[1] == 'I' && value[2] == 'F'
                    && value[3] == 'F' && value[8] == 'W' && value[9] == 'E' && value[10] == 'B' && value[11] == 'P';
            default -> false;
        };
    }

    private int unsigned(byte value) {
        return value & 0xff;
    }

    private String extension(String contentType) {
        return switch (contentType) {
            case "image/png" -> "png";
            case "image/webp" -> "webp";
            default -> "jpg";
        };
    }

    private String objectName(String extension) {
        return PrefixedIdGenerator.randomPart(32) + "." + extension;
    }

    private void safeDelete(String key) {
        try {
            storage.delete(key);
        } catch (RuntimeException exception) {
            log.warn("Could not clean up object storage key {}", key, exception);
        }
    }

    private record ValidatedUpload(byte[] content, String contentType, String extension) {
    }
}
