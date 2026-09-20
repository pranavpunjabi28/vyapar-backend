package com.bbu.vyaparbackend.catalog;

import com.bbu.vyaparbackend.file.MediaApi;

import java.util.List;
import java.util.function.Function;

final class CatalogMapper {
    private CatalogMapper() {
    }

    static CatalogApi.CategoryView toView(Category category) {
        return new CatalogApi.CategoryView(category.getId(), category.getName(), category.getDisplayOrder());
    }

    static CatalogApi.ProductView toView(CatalogQueryService.ProductDetails details,
                                         Function<String, MediaApi.FileResponse> signer) {
        Product product = details.product();
        return new CatalogApi.ProductView(product.getId(), product.getName(), product.getSku(), product.getDescription(),
                product.getPrice(), product.getCategory() == null ? null : product.getCategory().getId(),
                details.images().stream().map(image -> toView(image, signer)).toList(), product.isActive(),
                details.addonGroupIds());
    }

    static CatalogApi.ProductImageView toView(ProductImage image,
                                              Function<String, MediaApi.FileResponse> signer) {
        MediaApi.FileResponse signed = signer.apply(image.getObjectKey());
        return new CatalogApi.ProductImageView(image.getId(), signed.key(), signed.downloadUrl(), signed.expiresAt(),
                image.getDisplayOrder());
    }

    static CatalogApi.AddonGroupView toView(CatalogQueryService.AddonGroupDetails details) {
        AddonGroup group = details.group();
        return new CatalogApi.AddonGroupView(group.getId(), group.getName(), group.getDisplayOrder(),
                group.getMaximumSelections(), details.options().stream().map(CatalogMapper::toView).toList());
    }

    static CatalogApi.AddonOptionView toView(AddonOption option) {
        return new CatalogApi.AddonOptionView(option.getId(), option.getName(), option.getPrice(),
                option.getDisplayOrder(), option.isActive());
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
