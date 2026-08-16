package com.bbu.vyaparbackend.catalog;

import java.math.BigDecimal;

public final class CatalogCommands {
    private CatalogCommands() {
    }

    public record CategoryData(String name, int displayOrder) {
    }

    public record ProductData(String name, String sku, String description, BigDecimal price, String categoryId,
                              boolean active) {
    }

    public record IngredientData(String name, String unit, BigDecimal lowStockThreshold) {
    }

    public record RecipeLine(String ingredientId, BigDecimal quantity) {
    }
}
