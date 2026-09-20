package com.bbu.vyaparbackend.catalog;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public final class CatalogApi {
    private CatalogApi() {
    }

    public record CategoryRequest(@NotBlank @Size(max = 120) String name) {
        CatalogCommands.CategoryData toCommand() {
            return new CatalogCommands.CategoryData(name);
        }
    }

    public record ProductRequest(@NotBlank @Size(max = 160) String name, @Size(max = 80) String sku,
                                 @Size(max = 1000) String description,
                                 @NotNull @DecimalMin("0") BigDecimal price, String categoryId, boolean active,
                                 @Size(max = 20) List<@NotBlank String> addonGroupIds) {
        CatalogCommands.ProductData toCommand() {
            return new CatalogCommands.ProductData(name, sku, description, price, categoryId, active,
                    addonGroupIds == null ? List.of() : addonGroupIds);
        }
    }

    public record ProductBatchRequest(@NotNull @Valid ProductRequest product,
                                      @NotEmpty @Size(max = 100) List<@Valid OutletAvailabilityRequest> outlets) {
    }

    public record OutletAvailabilityRequest(@NotBlank String outletId, boolean active) {
    }

    public record ProductOutletView(String outletId, String productId, boolean active) {
    }

    public record AddonGroupRequest(@NotBlank @Size(max = 120) String name, @Min(0) int displayOrder,
                                    @Min(1) @Max(20) int maximumSelections,
                                    @NotNull @Size(min = 1, max = 50) List<@Valid AddonOptionRequest> options) {
        CatalogCommands.AddonGroupData toCommand() {
            return new CatalogCommands.AddonGroupData(name, displayOrder, maximumSelections,
                    options.stream().map(AddonOptionRequest::toCommand).toList());
        }
    }

    public record AddonOptionRequest(@NotBlank @Size(max = 120) String name,
                                     @NotNull @DecimalMin("0") BigDecimal price,
                                     @Min(0) int displayOrder, boolean active) {
        CatalogCommands.AddonOptionData toCommand() {
            return new CatalogCommands.AddonOptionData(name, price, displayOrder, active);
        }
    }

    public record IngredientRequest(@NotBlank @Size(max = 160) String name, @NotBlank @Size(max = 20) String unit,
                                    @NotNull @DecimalMin("0") BigDecimal lowStockThreshold) {
        CatalogCommands.IngredientData toCommand() {
            return new CatalogCommands.IngredientData(name, unit, lowStockThreshold);
        }
    }

    public record RecipeLine(@NotNull String ingredientId,
                             @NotNull @DecimalMin(value = "0", inclusive = false) BigDecimal quantity) {
        CatalogCommands.RecipeLine toCommand() {
            return new CatalogCommands.RecipeLine(ingredientId, quantity);
        }
    }

    public record CategoryView(String id, String name, int displayOrder) {
    }

    public record ProductView(String id, String name, String sku, String description, BigDecimal price,
                              String categoryId, List<ProductImageView> images, boolean active,
                              List<String> addonGroupIds) {
    }

    public record ProductImageView(String id, String key, String downloadUrl, Instant expiresAt, int displayOrder) {
    }

    public record AddonGroupView(String id, String name, int displayOrder, int maximumSelections,
                                 List<AddonOptionView> options) {
    }

    public record AddonOptionView(String id, String name, BigDecimal price, int displayOrder, boolean active) {
    }

    public record IngredientView(String id, String name, String unit, BigDecimal lowStockThreshold) {
    }

    public record RecipeView(String id, String ingredientId, String ingredientName, String unit, BigDecimal quantity) {
    }
}
