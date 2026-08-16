package com.bbu.vyaparbackend.catalog;

import com.bbu.vyaparbackend.business.Outlet;
import com.bbu.vyaparbackend.shared.ApiException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CatalogManagementService {
    private final CategoryRepository categories;
    private final ProductRepository products;
    private final IngredientRepository ingredients;
    private final RecipeComponentRepository recipes;
    private final CatalogQueryService queries;

    CatalogManagementService(CategoryRepository categories, ProductRepository products,
                             IngredientRepository ingredients, RecipeComponentRepository recipes,
                             CatalogQueryService queries) {
        this.categories = categories;
        this.products = products;
        this.ingredients = ingredients;
        this.recipes = recipes;
        this.queries = queries;
    }

    @Transactional
    public Category saveCategory(Outlet outlet, String categoryId, CatalogCommands.CategoryData command) {
        Category category = categoryId == null ? new Category() : categories.findById(categoryId)
                .filter(value -> value.getOutlet().getId().equals(outlet.getId()))
                .orElseThrow(() -> ApiException.notFound("Category"));
        category.setOutlet(outlet);
        category.setName(command.name());
        category.setDisplayOrder(command.displayOrder());
        return categories.save(category);
    }

    @Transactional
    public void archiveCategory(Outlet outlet, String categoryId) {
        Category category = categories.findById(categoryId)
                .filter(value -> !value.isArchived() && value.getOutlet().getId().equals(outlet.getId()))
                .orElseThrow(() -> ApiException.notFound("Category"));
        category.setArchived(true);
    }

    @Transactional
    public Product saveProduct(Outlet outlet, String productId, CatalogCommands.ProductData command) {
        Product product = productId == null ? new Product() : queries.product(outlet, productId);
        product.setOutlet(outlet);
        product.setName(command.name());
        product.setSku(command.sku());
        product.setDescription(command.description());
        product.setPrice(command.price());
        product.setActive(command.active());
        product.setCategory(command.categoryId() == null ? null : categories.findById(command.categoryId())
                .filter(category -> category.getOutlet().getId().equals(outlet.getId()))
                .orElseThrow(() -> ApiException.notFound("Category")));
        return products.save(product);
    }

    @Transactional
    public void archiveProduct(Outlet outlet, String productId) {
        queries.product(outlet, productId).setArchived(true);
    }

    @Transactional
    public void setProductImage(Outlet outlet, String productId, String key) {
        Product product = queries.product(outlet, productId);
        product.setImageKey(key);
    }

    @Transactional
    public Ingredient saveIngredient(Outlet outlet, String ingredientId, CatalogCommands.IngredientData command) {
        Ingredient ingredient = ingredientId == null ? new Ingredient() : queries.ingredient(outlet, ingredientId);
        ingredient.setOutlet(outlet);
        ingredient.setName(command.name());
        ingredient.setUnit(command.unit());
        ingredient.setLowStockThreshold(command.lowStockThreshold());
        return ingredients.save(ingredient);
    }

    @Transactional
    public void archiveIngredient(Outlet outlet, String ingredientId) {
        queries.ingredient(outlet, ingredientId).setArchived(true);
    }

    @Transactional
    public List<RecipeComponent> replaceRecipe(Outlet outlet, String productId,
                                               List<CatalogCommands.RecipeLine> lines) {
        Product product = queries.product(outlet, productId);
        recipes.deleteAllByProductId(productId);
        return lines.stream().map(line -> {
            RecipeComponent component = new RecipeComponent();
            component.setProduct(product);
            component.setIngredient(queries.ingredient(outlet, line.ingredientId()));
            component.setQuantity(line.quantity());
            return recipes.save(component);
        }).toList();
    }
}
