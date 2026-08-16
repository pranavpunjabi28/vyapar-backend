package com.bbu.vyaparbackend.file;

import com.bbu.vyaparbackend.auth.CurrentUser;
import com.bbu.vyaparbackend.business.Membership;
import com.bbu.vyaparbackend.business.Outlet;
import com.bbu.vyaparbackend.business.Role;
import com.bbu.vyaparbackend.business.TenantAccess;
import com.bbu.vyaparbackend.shared.ApiEndpoints;
import com.bbu.vyaparbackend.shared.JsonKeys;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;


@RestController
@RequestMapping(ApiEndpoints.Media.ROOT)
@ConditionalOnProperty(name = "app.storage.enabled", havingValue = "true", matchIfMissing = true)
public class MediaController {
    private final MediaService media;
    private final CurrentUser current;
    private final TenantAccess access;

    public MediaController(MediaService media, CurrentUser current, TenantAccess access) {
        this.media = media;
        this.current = current;
        this.access = access;
    }

    @PostMapping(value = ApiEndpoints.Media.BUSINESS_LOGO, consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    MediaApi.FileResponse logo(Authentication authentication, @PathVariable String businessId,
                               @RequestPart(JsonKeys.FILE) MultipartFile file) {
        Membership actor = access.business(current.require(authentication), businessId, Role.OWNER);
        return media.replaceBusinessLogo(actor, file);
    }

    @GetMapping(ApiEndpoints.Media.BUSINESS_LOGO)
    MediaApi.FileResponse logo(Authentication authentication, @PathVariable String businessId) {
        Membership actor = access.business(current.require(authentication), businessId);
        return media.businessLogo(actor);
    }

    @PostMapping(value = ApiEndpoints.Media.PRODUCT_IMAGE, consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    MediaApi.FileResponse product(Authentication authentication, @PathVariable String outletId,
                                  @PathVariable String productId, @RequestPart(JsonKeys.FILE) MultipartFile file) {
        Outlet outlet = access.outlet(current.require(authentication), outletId, Role.OWNER, Role.MANAGER);
        return media.replaceProductImage(outlet, productId, file);
    }

    @GetMapping(ApiEndpoints.Media.PRODUCT_IMAGE)
    MediaApi.FileResponse product(Authentication authentication, @PathVariable String outletId,
                                  @PathVariable String productId) {
        Outlet outlet = access.outlet(current.require(authentication), outletId);
        return media.productImage(outlet, productId);
    }
}
