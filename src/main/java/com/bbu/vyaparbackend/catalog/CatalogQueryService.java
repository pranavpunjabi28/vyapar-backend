package com.bbu.vyaparbackend.catalog;

import com.bbu.vyaparbackend.business.Outlet;
import com.bbu.vyaparbackend.shared.ApiException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.LinkedHashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.bbu.vyaparbackend.shared.Pageables.requireAllowedSort;

@Service
public class CatalogQueryService {
    private final CategoryRepository categories;
    private final ProductRepository products;
    private final IngredientRepository ingredients;
    private final RecipeComponentRepository recipes;
    private final ProductImageRepository productImages;
    private final AddonGroupRepository addonGroups;
    private final AddonOptionRepository addonOptions;
    private final ProductAddonGroupRepository productAddonGroups;

    CatalogQueryService(CategoryRepository categories, ProductRepository products,
                        IngredientRepository ingredients, RecipeComponentRepository recipes,
                        ProductImageRepository productImages, AddonGroupRepository addonGroups,
                        AddonOptionRepository addonOptions, ProductAddonGroupRepository productAddonGroups) {
        this.categories = categories;
        this.products = products;
        this.ingredients = ingredients;
        this.recipes = recipes;
        this.productImages = productImages;
        this.addonGroups = addonGroups;
        this.addonOptions = addonOptions;
        this.productAddonGroups = productAddonGroups;
    }

    @Transactional(readOnly = true)
    public List<Category> categories(Outlet outlet) {
        return categories.findAllByOutletIdAndArchivedFalseOrderByDisplayOrderAscNameAsc(outlet.getId());
    }

    @Transactional(readOnly = true)
    public Page<ProductDetails> products(Outlet outlet, String search, String categoryId, Pageable pageable) {
        pageable = requireAllowedSort(pageable, Set.of("id", "name", "price", "createdAt"), Sort.by("name"));
        String normalizedSearch = search == null ? "" : search;
        Page<Product> page = categoryId == null || categoryId.isBlank()
                ? products.findAllByOutletIdAndArchivedFalseAndNameContainingIgnoreCase(outlet.getId(),
                normalizedSearch, pageable)
                : products.findAllByOutletIdAndCategoryIdAndArchivedFalseAndNameContainingIgnoreCase(outlet.getId(),
                categoryId, normalizedSearch, pageable);
        if (page.isEmpty()) return page.map(product -> new ProductDetails(product, List.of(), List.of()));
        List<String> productIds = page.getContent().stream().map(Product::getId).toList();
        Map<String, List<ProductImage>> imagesByProduct = productImages
                .findAllByProductIdInAndArchivedFalseOrderByProductIdAscDisplayOrderAscIdAsc(productIds).stream()
                .collect(Collectors.groupingBy(image -> image.getProduct().getId()));
        Map<String, List<ProductAddonGroup>> groupsByProduct = productAddonGroups
                .findAllByProductIdInAndArchivedFalse(productIds).stream()
                .filter(assignment -> !assignment.getAddonGroup().isArchived())
                .collect(Collectors.groupingBy(assignment -> assignment.getProduct().getId()));
        return page.map(product -> new ProductDetails(product,
                imagesByProduct.getOrDefault(product.getId(), List.of()),
                groupsByProduct.getOrDefault(product.getId(), List.of()).stream()
                        .map(assignment -> assignment.getAddonGroup().getId()).sorted().toList()));
    }

    @Transactional(readOnly = true)
    public Product product(Outlet outlet, String productId) {
        return products.findById(productId)
                .filter(product -> !product.isArchived() && product.getOutlet().getId().equals(outlet.getId()))
                .orElseThrow(() -> ApiException.notFound("Product"));
    }

    @Transactional(readOnly = true)
    public ProductDetails productDetails(Outlet outlet, String productId) {
        Product product = product(outlet, productId);
        return new ProductDetails(product,
                productImages.findAllByProductIdAndArchivedFalseOrderByDisplayOrderAscIdAsc(productId),
                productAddonGroups.findAllByProductIdAndArchivedFalse(productId).stream()
                        .filter(assignment -> !assignment.getAddonGroup().isArchived())
                        .map(assignment -> assignment.getAddonGroup().getId()).sorted().toList());
    }

    @Transactional(readOnly = true)
    public List<ProductImage> productImages(Outlet outlet, String productId) {
        product(outlet, productId);
        return productImages.findAllByProductIdAndArchivedFalseOrderByDisplayOrderAscIdAsc(productId);
    }

    @Transactional(readOnly = true)
    public boolean productImageKeyInUse(String objectKey) {
        return productImages.existsByObjectKeyAndArchivedFalse(objectKey);
    }

    @Transactional(readOnly = true)
    public List<AddonGroupDetails> addonGroups(Outlet outlet) {
        List<AddonGroup> groups = addonGroups.findAllByOutletIdAndArchivedFalseOrderByDisplayOrderAscNameAsc(outlet.getId());
        if (groups.isEmpty()) return List.of();
        Map<String, List<AddonOption>> optionsByGroup = addonOptions
                .findAllByAddonGroupIdInAndArchivedFalseOrderByAddonGroupIdAscDisplayOrderAscNameAsc(
                        groups.stream().map(AddonGroup::getId).toList()).stream()
                .collect(Collectors.groupingBy(option -> option.getAddonGroup().getId()));
        return groups.stream().map(group -> new AddonGroupDetails(group,
                optionsByGroup.getOrDefault(group.getId(), List.of()))).toList();
    }

    @Transactional(readOnly = true)
    public AddonGroup addonGroup(Outlet outlet, String groupId) {
        return addonGroups.findById(groupId)
                .filter(group -> !group.isArchived() && group.getOutlet().getId().equals(outlet.getId()))
                .orElseThrow(() -> ApiException.notFound("Add-on group"));
    }

    @Transactional(readOnly = true)
    public List<AddonOption> addonOptions(Outlet outlet, String groupId) {
        addonGroup(outlet, groupId);
        return addonOptions.findAllByAddonGroupIdAndArchivedFalseOrderByDisplayOrderAscNameAsc(groupId);
    }

    @Transactional(readOnly = true)
    public List<SelectedAddon> selectedAddons(Outlet outlet, Product product, List<String> optionIds) {
        if (optionIds == null || optionIds.isEmpty()) return List.of();
        if (new HashSet<>(optionIds).size() != optionIds.size()) {
            throw ApiException.invalid("An add-on option can only be selected once per order item");
        }
        Set<String> allowedGroupIds = productAddonGroups.findAllByProductIdAndArchivedFalse(product.getId()).stream()
                .filter(assignment -> !assignment.getAddonGroup().isArchived())
                .map(assignment -> assignment.getAddonGroup().getId())
                .collect(Collectors.toSet());
        Map<String, AddonOption> selectedById = addonOptions.findAllById(optionIds).stream()
                .collect(Collectors.toMap(AddonOption::getId, option -> option, (first, ignored) -> first,
                        LinkedHashMap::new));
        if (selectedById.size() != optionIds.size()) throw ApiException.invalid("One or more add-on options are invalid");

        Map<String, Integer> selectionsByGroup = new java.util.HashMap<>();
        List<SelectedAddon> selected = optionIds.stream().map(optionId -> {
            AddonOption option = selectedById.get(optionId);
            AddonGroup group = option.getAddonGroup();
            if (option.isArchived() || !option.isActive() || group.isArchived()
                    || !group.getOutlet().getId().equals(outlet.getId()) || !allowedGroupIds.contains(group.getId())) {
                throw ApiException.invalid("Selected add-on option is not available for this product");
            }
            int count = selectionsByGroup.merge(group.getId(), 1, Integer::sum);
            if (count > group.getMaximumSelections()) {
                throw ApiException.invalid(group.getName() + " allows at most " + group.getMaximumSelections()
                        + " selection(s)");
            }
            return new SelectedAddon(group, option);
        }).toList();
        return List.copyOf(selected);
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

    public record ProductDetails(Product product, List<ProductImage> images, List<String> addonGroupIds) {
    }

    public record AddonGroupDetails(AddonGroup group, List<AddonOption> options) {
    }

    public record SelectedAddon(AddonGroup group, AddonOption option) {
    }
}
