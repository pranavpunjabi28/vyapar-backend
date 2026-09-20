package com.bbu.vyaparbackend.catalog;

import java.math.BigDecimal;
import java.util.List;

public final class CatalogCommands {
    private CatalogCommands() {
    }

    public record CategoryData(String name) {
    }

    public record ProductData(String name, String sku, String description, BigDecimal price, String categoryId,
                              boolean active, List<String> addonGroupIds) {
    }

    public record AddonGroupData(String name, int displayOrder, int maximumSelections,
                                 List<AddonOptionData> options) {
    }

    public record AddonOptionData(String name, BigDecimal price, int displayOrder, boolean active) {
    }

    public record IngredientData(String name, String unit, BigDecimal lowStockThreshold) {
    }

    public record RecipeLine(String ingredientId, BigDecimal quantity) {
    }
}
