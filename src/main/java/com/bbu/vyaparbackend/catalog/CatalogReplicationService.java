package com.bbu.vyaparbackend.catalog;

import com.bbu.vyaparbackend.business.Outlet;
import com.bbu.vyaparbackend.shared.ApiException;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

@Service
public class CatalogReplicationService {
    private final CategoryRepository categories;
    private final ProductRepository products;
    private final ProductImageRepository productImages;
    private final AddonGroupRepository addonGroups;
    private final AddonOptionRepository addonOptions;
    private final ProductAddonGroupRepository productAddonGroups;
    private final CatalogQueryService queries;
    private final CatalogManagementService management;
    private final EntityManager entityManager;

    CatalogReplicationService(CategoryRepository categories, ProductRepository products,
                              ProductImageRepository productImages, AddonGroupRepository addonGroups,
                              AddonOptionRepository addonOptions, ProductAddonGroupRepository productAddonGroups,
                              CatalogQueryService queries, CatalogManagementService management,
                              EntityManager entityManager) {
        this.categories = categories;
        this.products = products;
        this.productImages = productImages;
        this.addonGroups = addonGroups;
        this.addonOptions = addonOptions;
        this.productAddonGroups = productAddonGroups;
        this.queries = queries;
        this.management = management;
        this.entityManager = entityManager;
    }

    @Transactional
    public void copyMenu(Outlet source, Outlet target) {
        if (!source.getBusiness().getId().equals(target.getBusiness().getId())) {
            throw ApiException.invalid("Menu catalogs can only be copied within the same business");
        }

        Map<String, Category> categoryCopies = new HashMap<>();
        for (Category sourceCategory : categories.findAllByOutletIdAndArchivedFalseOrderByDisplayOrderAscNameAsc(source.getId())) {
            Category copy = new Category();
            copy.setOutlet(target);
            copy.setName(sourceCategory.getName());
            copy.setDisplayOrder(sourceCategory.getDisplayOrder());
            categoryCopies.put(sourceCategory.getId(), categories.save(copy));
        }

        Map<String, AddonGroup> groupCopies = new HashMap<>();
        List<AddonGroup> sourceGroups = addonGroups
                .findAllByOutletIdAndArchivedFalseOrderByDisplayOrderAscNameAsc(source.getId());
        Map<String, List<AddonOption>> optionsByGroup = groupOptions(sourceGroups);
        for (AddonGroup sourceGroup : sourceGroups) {
            groupCopies.put(sourceGroup.getId(), cloneGroup(sourceGroup, optionsByGroup.getOrDefault(
                    sourceGroup.getId(), List.of()), target));
        }

        List<Product> sourceProducts = products.findAllByOutletIdAndArchivedFalseOrderByCreatedAtAscIdAsc(source.getId());
        if (sourceProducts.isEmpty()) return;
        List<String> sourceProductIds = sourceProducts.stream().map(Product::getId).toList();
        Map<String, List<ProductImage>> imagesByProduct = productImages
                .findAllByProductIdInAndArchivedFalseOrderByProductIdAscDisplayOrderAscIdAsc(sourceProductIds).stream()
                .collect(java.util.stream.Collectors.groupingBy(image -> image.getProduct().getId()));
        Map<String, List<ProductAddonGroup>> assignmentsByProduct = productAddonGroups
                .findAllByProductIdInAndArchivedFalse(sourceProductIds).stream()
                .collect(java.util.stream.Collectors.groupingBy(assignment -> assignment.getProduct().getId()));

        for (Product sourceProduct : sourceProducts) {
            Product copy = cloneProduct(sourceProduct, target,
                    sourceProduct.getCategory() == null ? null : categoryCopies.get(sourceProduct.getCategory().getId()));
            products.save(copy);
            copyImages(imagesByProduct.getOrDefault(sourceProduct.getId(), List.of()), copy);
            for (ProductAddonGroup assignment : assignmentsByProduct.getOrDefault(sourceProduct.getId(), List.of())) {
                AddonGroup copiedGroup = groupCopies.get(assignment.getAddonGroup().getId());
                if (copiedGroup != null) assignGroup(copy, copiedGroup);
            }
        }
    }

    @Transactional
    public List<OutletProduct> createAcrossOutlets(Outlet source, List<OutletAvailability> availability,
                                                   CatalogCommands.ProductData command) {
        if (availability.isEmpty()) throw ApiException.invalid("Select at least one outlet");
        if (new HashSet<>(availability.stream().map(item -> item.outlet().getId()).toList()).size()
                != availability.size()) {
            throw ApiException.invalid("Each outlet can only be selected once");
        }

        Category sourceCategory = command.categoryId() == null ? null : categories.findById(command.categoryId())
                .filter(category -> !category.isArchived() && category.getOutlet().getId().equals(source.getId()))
                .orElseThrow(() -> ApiException.notFound("Category"));
        List<AddonGroup> sourceGroups = command.addonGroupIds().stream().distinct()
                .map(groupId -> queries.addonGroup(source, groupId)).toList();
        Map<String, List<AddonOption>> sourceOptions = groupOptions(sourceGroups);

        List<OutletProduct> created = new ArrayList<>();
        for (OutletAvailability selection : availability) {
            Outlet target = selection.outlet();
            if (!source.getBusiness().getId().equals(target.getBusiness().getId())) {
                throw ApiException.invalid("All selected outlets must belong to the same business");
            }
            Category targetCategory = sourceCategory == null ? null : resolveCategory(sourceCategory, target);
            List<String> targetGroupIds = sourceGroups.stream()
                    .map(group -> resolveGroup(group, sourceOptions.getOrDefault(group.getId(), List.of()), target).getId())
                    .toList();
            CatalogCommands.ProductData targetCommand = new CatalogCommands.ProductData(command.name(), command.sku(),
                    command.description(), command.price(), targetCategory == null ? null : targetCategory.getId(),
                    selection.active(), targetGroupIds);
            Product product = management.saveProduct(target, null, targetCommand);
            created.add(new OutletProduct(target.getId(), product.getId(), product.isActive()));
        }
        return List.copyOf(created);
    }

    private Category resolveCategory(Category source, Outlet target) {
        if (source.getOutlet().getId().equals(target.getId())) return source;
        lockCatalog(target.getId());
        return categories.findByOutletIdAndNameIgnoreCaseAndArchivedFalse(target.getId(), source.getName())
                .orElseGet(() -> {
                    Category copy = new Category();
                    copy.setOutlet(target);
                    copy.setName(source.getName());
                    copy.setDisplayOrder(categories.maximumDisplayOrder(target.getId()) + 1);
                    return categories.save(copy);
                });
    }

    private AddonGroup resolveGroup(AddonGroup source, List<AddonOption> sourceOptions, Outlet target) {
        if (source.getOutlet().getId().equals(target.getId())) return source;
        return addonGroups.findByOutletIdAndName(target.getId(), source.getName()).map(existing -> {
            if (existing.isArchived()) existing.setArchived(false);
            return existing;
        }).orElseGet(() -> cloneGroup(source, sourceOptions, target));
    }

    private AddonGroup cloneGroup(AddonGroup source, List<AddonOption> sourceOptions, Outlet target) {
        AddonGroup copy = new AddonGroup();
        copy.setOutlet(target);
        copy.setName(source.getName());
        copy.setDisplayOrder(source.getDisplayOrder());
        copy.setMaximumSelections(source.getMaximumSelections());
        addonGroups.save(copy);
        for (AddonOption sourceOption : sourceOptions) {
            AddonOption option = new AddonOption();
            option.setAddonGroup(copy);
            option.setName(sourceOption.getName());
            option.setPrice(sourceOption.getPrice());
            option.setDisplayOrder(sourceOption.getDisplayOrder());
            option.setActive(sourceOption.isActive());
            addonOptions.save(option);
        }
        return copy;
    }

    private Product cloneProduct(Product source, Outlet target, Category category) {
        Product copy = new Product();
        copy.setOutlet(target);
        copy.setCategory(category);
        copy.setName(source.getName());
        copy.setSku(source.getSku());
        copy.setDescription(source.getDescription());
        copy.setPrice(source.getPrice());
        copy.setActive(source.isActive());
        return copy;
    }

    private void copyImages(List<ProductImage> sourceImages, Product product) {
        for (ProductImage sourceImage : sourceImages) {
            ProductImage image = new ProductImage();
            image.setProduct(product);
            image.setObjectKey(sourceImage.getObjectKey());
            image.setDisplayOrder(sourceImage.getDisplayOrder());
            productImages.save(image);
        }
    }

    private void assignGroup(Product product, AddonGroup group) {
        ProductAddonGroup assignment = new ProductAddonGroup();
        assignment.setProduct(product);
        assignment.setAddonGroup(group);
        productAddonGroups.save(assignment);
    }

    private Map<String, List<AddonOption>> groupOptions(List<AddonGroup> groups) {
        if (groups.isEmpty()) return Map.of();
        return addonOptions.findAllByAddonGroupIdInAndArchivedFalseOrderByAddonGroupIdAscDisplayOrderAscNameAsc(
                        groups.stream().map(AddonGroup::getId).toList()).stream()
                .collect(java.util.stream.Collectors.groupingBy(option -> option.getAddonGroup().getId()));
    }

    private void lockCatalog(String outletId) {
        entityManager.createNativeQuery(
                        "select pg_advisory_xact_lock(hashtextextended(cast(?1 as text), 0))")
                .setParameter(1, outletId)
                .getSingleResult();
    }

    public record OutletAvailability(Outlet outlet, boolean active) {
    }

    public record OutletProduct(String outletId, String productId, boolean active) {
    }
}
