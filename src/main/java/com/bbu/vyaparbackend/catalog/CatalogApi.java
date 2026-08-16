package com.bbu.vyaparbackend.catalog;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public final class CatalogApi {
    private CatalogApi() {
    }

    public record CategoryRequest(@NotBlank @Size(max = 120) String name, @Min(0) int displayOrder) {
        CatalogCommands.CategoryData toCommand() {
            return new CatalogCommands.CategoryData(name, displayOrder);
        }
    }

    public record ProductRequest(@NotBlank @Size(max = 160) String name, @Size(max = 80) String sku,
                                 @Size(max = 1000) String description,
                                 @NotNull @DecimalMin("0") BigDecimal price, String categoryId, boolean active) {
        CatalogCommands.ProductData toCommand() {
            return new CatalogCommands.ProductData(name, sku, description, price, categoryId, active);
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
                              String categoryId,
                              String imageKey, boolean active) {
    }

    public record IngredientView(String id, String name, String unit, BigDecimal lowStockThreshold) {
    }

    public record RecipeView(String id, String ingredientId, String ingredientName, String unit, BigDecimal quantity) {
    }
}
