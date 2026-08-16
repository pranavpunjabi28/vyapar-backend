package com.bbu.vyaparbackend.catalog;

import com.bbu.vyaparbackend.business.Outlet;
import com.bbu.vyaparbackend.shared.ApiException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

import static com.bbu.vyaparbackend.shared.Pageables.requireAllowedSort;

@Service
public class CatalogQueryService {
    private final CategoryRepository categories;
    private final ProductRepository products;
    private final IngredientRepository ingredients;
    private final RecipeComponentRepository recipes;

    CatalogQueryService(CategoryRepository categories, ProductRepository products,
                        IngredientRepository ingredients, RecipeComponentRepository recipes) {
        this.categories = categories;
        this.products = products;
        this.ingredients = ingredients;
        this.recipes = recipes;
    }

    @Transactional(readOnly = true)
    public List<Category> categories(Outlet outlet) {
        return categories.findAllByOutletIdAndArchivedFalseOrderByDisplayOrderAscNameAsc(outlet.getId());
    }

    @Transactional(readOnly = true)
    public Page<Product> products(Outlet outlet, String search, Pageable pageable) {
        pageable = requireAllowedSort(pageable, Set.of("id", "name", "price", "createdAt"), Sort.by("name"));
        return products.findAllByOutletIdAndArchivedFalseAndNameContainingIgnoreCase(outlet.getId(),
                search == null ? "" : search, pageable);
    }

    @Transactional(readOnly = true)
    public Product product(Outlet outlet, String productId) {
        return products.findById(productId)
                .filter(product -> !product.isArchived() && product.getOutlet().getId().equals(outlet.getId()))
                .orElseThrow(() -> ApiException.notFound("Product"));
    }

    @Transactional(readOnly = true)
    public Page<Ingredient> ingredients(Outlet outlet, String search, Pageable pageable) {
        pageable = requireAllowedSort(pageable, Set.of("id", "name", "lowStockThreshold", "createdAt"),
                Sort.by("name"));
        return ingredients.findAllByOutletIdAndArchivedFalseAndNameContainingIgnoreCase(outlet.getId(),
                search == null ? "" : search, pageable);
    }

    @Transactional(readOnly = true)
    public Ingredient ingredient(Outlet outlet, String ingredientId) {
        return ingredients.findById(ingredientId)
                .filter(ingredient -> !ingredient.isArchived() && ingredient.getOutlet().getId().equals(outlet.getId()))
                .orElseThrow(() -> ApiException.notFound("Ingredient"));
    }

    @Transactional(readOnly = true)
    public List<RecipeComponent> recipe(Outlet outlet, String productId) {
        product(outlet, productId);
        return recipes.findAllByProductIdAndArchivedFalse(productId);
    }
}
