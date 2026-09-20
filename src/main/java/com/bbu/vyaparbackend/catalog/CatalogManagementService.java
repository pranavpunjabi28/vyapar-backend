package com.bbu.vyaparbackend.catalog;

import com.bbu.vyaparbackend.business.Outlet;
import com.bbu.vyaparbackend.shared.ApiException;
import com.bbu.vyaparbackend.shared.LogMessages;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import jakarta.persistence.EntityManager;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class CatalogManagementService {
    private static final Logger log = LoggerFactory.getLogger(CatalogManagementService.class);

    private final CategoryRepository categories;
    private final ProductRepository products;
    private final IngredientRepository ingredients;
    private final RecipeComponentRepository recipes;
    private final ProductImageRepository productImages;
    private final AddonGroupRepository addonGroups;
    private final AddonOptionRepository addonOptions;
    private final ProductAddonGroupRepository productAddonGroups;
    private final CatalogQueryService queries;
    private final EntityManager entityManager;

    CatalogManagementService(CategoryRepository categories, ProductRepository products,
                             IngredientRepository ingredients, RecipeComponentRepository recipes,
                             ProductImageRepository productImages, AddonGroupRepository addonGroups,
                             AddonOptionRepository addonOptions, ProductAddonGroupRepository productAddonGroups,
                             CatalogQueryService queries, EntityManager entityManager) {
        this.categories = categories;
        this.products = products;
        this.ingredients = ingredients;
        this.recipes = recipes;
        this.productImages = productImages;
        this.addonGroups = addonGroups;
        this.addonOptions = addonOptions;
        this.productAddonGroups = productAddonGroups;
        this.queries = queries;
        this.entityManager = entityManager;
    }

    @Transactional
    public Category saveCategory(Outlet outlet, String categoryId, CatalogCommands.CategoryData command) {
        boolean creating = categoryId == null;
        Category category = creating ? new Category() : categories.findById(categoryId)
                .filter(value -> value.getOutlet().getId().equals(outlet.getId()))
                .orElseThrow(() -> ApiException.notFound("Category"));
        String name = command.name().trim();
        lockCategoryPositions(outlet.getId());
        categories.findByOutletIdAndNameIgnoreCaseAndArchivedFalse(outlet.getId(), name)
                .filter(existing -> !existing.getId().equals(category.getId()))
                .ifPresent(existing -> {
                    throw ApiException.conflict("An active category with this name already exists");
                });
        category.setOutlet(outlet);
        category.setName(name);
        if (creating) {
            category.setDisplayOrder(categories.maximumDisplayOrder(outlet.getId()) + 1);
        }
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
        boolean creating = productId == null;
        Product product = productId == null ? new Product() : queries.product(outlet, productId);
        product.setOutlet(outlet);
        product.setName(command.name().trim());
        product.setSku(optional(command.sku()));
        product.setDescription(optional(command.description()));
        product.setPrice(command.price());
        product.setActive(command.active());
        product.setCategory(command.categoryId() == null ? null : categories.findById(command.categoryId())
                .filter(category -> !category.isArchived() && category.getOutlet().getId().equals(outlet.getId()))
                .orElseThrow(() -> ApiException.notFound("Category")));
        Product saved = products.save(product);
        replaceProductAddonGroups(outlet, saved, command.addonGroupIds());
        log.info(LogMessages.PRODUCT_SAVED, saved.getId(), outlet.getId(), creating ? "created" : "updated",
                saved.isActive(), command.addonGroupIds() == null ? 0 : command.addonGroupIds().size());
        return saved;
    }

    @Transactional
    public void archiveProduct(Outlet outlet, String productId) {
        queries.product(outlet, productId).setArchived(true);
    }

    @Transactional
    public ProductImage addProductImage(Outlet outlet, String productId, String key, int displayOrder) {
        Product product = queries.product(outlet, productId);
        ProductImage image = new ProductImage();
        image.setProduct(product);
        image.setObjectKey(key);
        image.setDisplayOrder(displayOrder);
        return productImages.save(image);
    }

    @Transactional
    public ProductImage archiveProductImage(Outlet outlet, String productId, String imageId) {
        queries.product(outlet, productId);
        ProductImage image = productImages.findById(imageId)
                .filter(value -> !value.isArchived() && value.getProduct().getId().equals(productId))
                .orElseThrow(() -> ApiException.notFound("Product image"));
        image.setArchived(true);
        return image;
    }

    @Transactional
    public AddonGroup saveAddonGroup(Outlet outlet, String groupId, CatalogCommands.AddonGroupData command) {
        boolean creating = groupId == null;
        if (command.options().size() < command.maximumSelections()) {
            throw ApiException.invalid("Maximum selections cannot exceed the number of add-on options");
        }
        if (new HashSet<>(command.options().stream()
                .map(option -> option.name().trim().toLowerCase(Locale.ROOT)).toList())
                .size() != command.options().size()) {
            throw ApiException.invalid("Add-on option names must be unique within a group");
        }
        AddonGroup group = groupId == null ? new AddonGroup() : queries.addonGroup(outlet, groupId);
        group.setOutlet(outlet);
        group.setName(command.name().trim());
        group.setDisplayOrder(command.displayOrder());
        group.setMaximumSelections(command.maximumSelections());
        AddonGroup saved = addonGroups.save(group);
        addonOptions.findAllByAddonGroupIdAndArchivedFalseOrderByDisplayOrderAscNameAsc(saved.getId())
                .forEach(option -> option.setArchived(true));
        command.options().forEach(optionData -> {
            AddonOption option = new AddonOption();
            option.setAddonGroup(saved);
            option.setName(optionData.name().trim());
            option.setPrice(optionData.price());
            option.setDisplayOrder(optionData.displayOrder());
            option.setActive(optionData.active());
            addonOptions.save(option);
        });
        log.info(LogMessages.ADDON_GROUP_SAVED, saved.getId(), outlet.getId(),
                creating ? "created" : "updated", command.options().size());
        return saved;
    }

    @Transactional
    public void archiveAddonGroup(Outlet outlet, String groupId) {
        queries.addonGroup(outlet, groupId).setArchived(true);
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

    private void replaceProductAddonGroups(Outlet outlet, Product product, List<String> groupIds) {
        Set<String> requestedIds = new LinkedHashSet<>(groupIds == null ? List.of() : groupIds);
        Map<String, AddonGroup> requestedGroups = requestedIds.stream()
                .collect(Collectors.toMap(Function.identity(), groupId -> queries.addonGroup(outlet, groupId),
                        (first, ignored) -> first, LinkedHashMap::new));
        Map<String, ProductAddonGroup> existingAssignments = productAddonGroups
                .findAllByProductId(product.getId()).stream()
                .collect(Collectors.toMap(assignment -> assignment.getAddonGroup().getId(), Function.identity()));

        existingAssignments.forEach((groupId, assignment) ->
                assignment.setArchived(!requestedGroups.containsKey(groupId)));
        requestedGroups.forEach((groupId, group) -> {
            ProductAddonGroup existing = existingAssignments.get(groupId);
            if (existing != null) {
                existing.setArchived(false);
                return;
            }
            ProductAddonGroup assignment = new ProductAddonGroup();
            assignment.setProduct(product);
            assignment.setAddonGroup(group);
            productAddonGroups.save(assignment);
        });
    }

    private String optional(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private void lockCategoryPositions(String outletId) {
        entityManager.createNativeQuery(
                        "select pg_advisory_xact_lock(hashtextextended(cast(?1 as text), 0))")
                .setParameter(1, outletId)
                .getSingleResult();
    }
}
