package com.bbu.vyaparbackend.catalog;

final class CatalogMapper {
    private CatalogMapper() {
    }

    static CatalogApi.CategoryView toView(Category category) {
        return new CatalogApi.CategoryView(category.getId(), category.getName(), category.getDisplayOrder());
    }

    static CatalogApi.ProductView toView(Product product) {
        return new CatalogApi.ProductView(product.getId(), product.getName(), product.getSku(), product.getDescription(),
                product.getPrice(), product.getCategory() == null ? null : product.getCategory().getId(),
                product.getImageKey(), product.isActive());
    }

    static CatalogApi.IngredientView toView(Ingredient ingredient) {
        return new CatalogApi.IngredientView(ingredient.getId(), ingredient.getName(), ingredient.getUnit(),
                ingredient.getLowStockThreshold());
    }

    static CatalogApi.RecipeView toView(RecipeComponent component) {
        return new CatalogApi.RecipeView(component.getId(), component.getIngredient().getId(),
                component.getIngredient().getName(), component.getIngredient().getUnit(), component.getQuantity());
    }
}
