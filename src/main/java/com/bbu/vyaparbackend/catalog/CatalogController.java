package com.bbu.vyaparbackend.catalog;

import com.bbu.vyaparbackend.auth.CurrentUser;
import com.bbu.vyaparbackend.business.Outlet;
import com.bbu.vyaparbackend.business.Role;
import com.bbu.vyaparbackend.business.TenantAccess;
import com.bbu.vyaparbackend.catalog.CatalogApi.RecipeLine;
import com.bbu.vyaparbackend.file.MediaService;
import com.bbu.vyaparbackend.shared.ApiEndpoints;
import com.bbu.vyaparbackend.shared.PageResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(ApiEndpoints.Catalog.ROOT)
public class CatalogController {
    private final CatalogQueryService queries;
    private final CatalogManagementService management;
    private final CatalogReplicationService replication;
    private final MediaService media;
    private final CurrentUser current;
    private final TenantAccess access;

    public CatalogController(CatalogQueryService queries, CatalogManagementService management,
                             CatalogReplicationService replication, MediaService media,
                             CurrentUser current, TenantAccess access) {
        this.queries = queries;
        this.management = management;
        this.replication = replication;
        this.media = media;
        this.current = current;
        this.access = access;
    }

    private Outlet outlet(Authentication authentication, String outletId, Role... roles) {
        return access.outlet(current.require(authentication), outletId, roles);
    }

    @GetMapping(ApiEndpoints.Catalog.CATEGORIES)
    List<CatalogApi.CategoryView> categories(Authentication authentication, @PathVariable String outletId) {
        return queries.categories(outlet(authentication, outletId)).stream().map(CatalogMapper::toView).toList();
    }

    @PostMapping(ApiEndpoints.Catalog.CATEGORIES)
    @ResponseStatus(HttpStatus.CREATED)
    CatalogApi.CategoryView category(Authentication authentication, @PathVariable String outletId,
                                     @Valid @RequestBody CatalogApi.CategoryRequest request) {
        return CatalogMapper.toView(management.saveCategory(
                outlet(authentication, outletId, Role.OWNER, Role.MANAGER), null, request.toCommand()));
    }

    @PutMapping(ApiEndpoints.Catalog.CATEGORY)
    CatalogApi.CategoryView category(Authentication authentication, @PathVariable String outletId, @PathVariable String id,
                                     @Valid @RequestBody CatalogApi.CategoryRequest request) {
        return CatalogMapper.toView(management.saveCategory(
                outlet(authentication, outletId, Role.OWNER, Role.MANAGER), id, request.toCommand()));
    }

    @DeleteMapping(ApiEndpoints.Catalog.CATEGORY)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void deleteCategory(Authentication authentication, @PathVariable String outletId, @PathVariable String id) {
        management.archiveCategory(outlet(authentication, outletId, Role.OWNER, Role.MANAGER), id);
    }

    @GetMapping(ApiEndpoints.Catalog.PRODUCTS)
    PageResponse<CatalogApi.ProductView> products(Authentication authentication, @PathVariable String outletId,
                                                  @RequestParam(defaultValue = "") String search,
                                                  @RequestParam(required = false) String categoryId,
                                                  Pageable pageable) {
        return PageResponse.from(queries.products(outlet(authentication, outletId), search, categoryId, pageable)
                .map(details -> CatalogMapper.toView(details, media::sign)));
    }

    @GetMapping(ApiEndpoints.Catalog.PRODUCT)
    CatalogApi.ProductView product(Authentication authentication, @PathVariable String outletId,
                                   @PathVariable String id) {
        Outlet outlet = outlet(authentication, outletId);
        return CatalogMapper.toView(queries.productDetails(outlet, id), media::sign);
    }

    @PostMapping(ApiEndpoints.Catalog.PRODUCTS)
    @ResponseStatus(HttpStatus.CREATED)
    CatalogApi.ProductView product(Authentication authentication, @PathVariable String outletId,
                                   @Valid @RequestBody CatalogApi.ProductRequest request) {
        Outlet outlet = outlet(authentication, outletId, Role.OWNER, Role.MANAGER);
        Product product = management.saveProduct(outlet, null, request.toCommand());
        return CatalogMapper.toView(queries.productDetails(outlet, product.getId()), media::sign);
    }

    @PostMapping(ApiEndpoints.Catalog.PRODUCTS_BATCH)
    @ResponseStatus(HttpStatus.CREATED)
    List<CatalogApi.ProductOutletView> products(Authentication authentication, @PathVariable String outletId,
                                                @Valid @RequestBody CatalogApi.ProductBatchRequest request) {
        Outlet source = outlet(authentication, outletId, Role.OWNER, Role.MANAGER);
        List<CatalogReplicationService.OutletAvailability> targets = request.outlets().stream().map(selection -> {
            Outlet target = outlet(authentication, selection.outletId(), Role.OWNER, Role.MANAGER);
            if (!target.getBusiness().getId().equals(source.getBusiness().getId())) {
                throw com.bbu.vyaparbackend.shared.ApiException.invalid(
                        "All selected outlets must belong to the same business");
            }
            return new CatalogReplicationService.OutletAvailability(target, selection.active());
        }).toList();
        return replication.createAcrossOutlets(source, targets, request.product().toCommand()).stream()
                .map(result -> new CatalogApi.ProductOutletView(result.outletId(), result.productId(), result.active()))
                .toList();
    }

    @PutMapping(ApiEndpoints.Catalog.PRODUCT)
    CatalogApi.ProductView product(Authentication authentication, @PathVariable String outletId, @PathVariable String id,
                                   @Valid @RequestBody CatalogApi.ProductRequest request) {
        Outlet outlet = outlet(authentication, outletId, Role.OWNER, Role.MANAGER);
        Product product = management.saveProduct(outlet, id, request.toCommand());
        return CatalogMapper.toView(queries.productDetails(outlet, product.getId()), media::sign);
    }

    @DeleteMapping(ApiEndpoints.Catalog.PRODUCT)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void deleteProduct(Authentication authentication, @PathVariable String outletId, @PathVariable String id) {
        management.archiveProduct(outlet(authentication, outletId, Role.OWNER, Role.MANAGER), id);
    }

    @GetMapping(ApiEndpoints.Catalog.ADDON_GROUPS)
    List<CatalogApi.AddonGroupView> addonGroups(Authentication authentication, @PathVariable String outletId) {
        return queries.addonGroups(outlet(authentication, outletId)).stream().map(CatalogMapper::toView).toList();
    }

    @PostMapping(ApiEndpoints.Catalog.ADDON_GROUPS)
    @ResponseStatus(HttpStatus.CREATED)
    CatalogApi.AddonGroupView addonGroup(Authentication authentication, @PathVariable String outletId,
                                         @Valid @RequestBody CatalogApi.AddonGroupRequest request) {
        Outlet outlet = outlet(authentication, outletId, Role.OWNER, Role.MANAGER);
        AddonGroup group = management.saveAddonGroup(outlet, null, request.toCommand());
        return CatalogMapper.toView(new CatalogQueryService.AddonGroupDetails(group,
                queries.addonOptions(outlet, group.getId())));
    }

    @PutMapping(ApiEndpoints.Catalog.ADDON_GROUP)
    CatalogApi.AddonGroupView addonGroup(Authentication authentication, @PathVariable String outletId,
                                         @PathVariable String id,
                                         @Valid @RequestBody CatalogApi.AddonGroupRequest request) {
        Outlet outlet = outlet(authentication, outletId, Role.OWNER, Role.MANAGER);
        AddonGroup group = management.saveAddonGroup(outlet, id, request.toCommand());
        return CatalogMapper.toView(new CatalogQueryService.AddonGroupDetails(group,
                queries.addonOptions(outlet, group.getId())));
    }

    @DeleteMapping(ApiEndpoints.Catalog.ADDON_GROUP)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void deleteAddonGroup(Authentication authentication, @PathVariable String outletId, @PathVariable String id) {
        management.archiveAddonGroup(outlet(authentication, outletId, Role.OWNER, Role.MANAGER), id);
    }

    @GetMapping(ApiEndpoints.Catalog.INGREDIENTS)
    PageResponse<CatalogApi.IngredientView> ingredients(Authentication authentication, @PathVariable String outletId,
                                                        @RequestParam(defaultValue = "") String search,
                                                        Pageable pageable) {
        return PageResponse.from(queries.ingredients(outlet(authentication, outletId), search, pageable)
                .map(CatalogMapper::toView));
    }

    @PostMapping(ApiEndpoints.Catalog.INGREDIENTS)
    CatalogApi.IngredientView ingredient(Authentication authentication, @PathVariable String outletId,
                                         @Valid @RequestBody CatalogApi.IngredientRequest request) {
        return CatalogMapper.toView(management.saveIngredient(
                outlet(authentication, outletId, Role.OWNER, Role.MANAGER, Role.INVENTORY), null,
                request.toCommand()));
    }

    @PutMapping(ApiEndpoints.Catalog.INGREDIENT)
    CatalogApi.IngredientView ingredient(Authentication authentication, @PathVariable String outletId,
                                         @PathVariable String id,
                                         @Valid @RequestBody CatalogApi.IngredientRequest request) {
        return CatalogMapper.toView(management.saveIngredient(
                outlet(authentication, outletId, Role.OWNER, Role.MANAGER, Role.INVENTORY), id,
                request.toCommand()));
    }

    @DeleteMapping(ApiEndpoints.Catalog.INGREDIENT)
    void deleteIngredient(Authentication authentication, @PathVariable String outletId, @PathVariable String id) {
        management.archiveIngredient(outlet(authentication, outletId, Role.OWNER, Role.MANAGER, Role.INVENTORY), id);
    }

    @GetMapping(ApiEndpoints.Catalog.RECIPE)
    List<CatalogApi.RecipeView> recipe(Authentication authentication, @PathVariable String outletId,
                                       @PathVariable String id) {
        return queries.recipe(outlet(authentication, outletId), id).stream().map(CatalogMapper::toView).toList();
    }

    @PutMapping(ApiEndpoints.Catalog.RECIPE)
    List<CatalogApi.RecipeView> recipe(Authentication authentication, @PathVariable String outletId,
                                       @PathVariable String id, @RequestBody List<@Valid RecipeLine> request) {
        Outlet outlet = outlet(authentication, outletId, Role.OWNER, Role.MANAGER, Role.INVENTORY);
        return management.replaceRecipe(outlet, id, request.stream().map(CatalogApi.RecipeLine::toCommand).toList())
                .stream().map(CatalogMapper::toView).toList();
    }
}
