package com.bbu.vyaparbackend.catalog;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Collection;
import java.util.Optional;

interface CategoryRepository extends JpaRepository<Category, String> {
    List<Category> findAllByOutletIdAndArchivedFalseOrderByDisplayOrderAscNameAsc(String outletId);

    @Query("select coalesce(max(c.displayOrder), -1) from Category c where c.outlet.id=:outletId and c.archived=false")
    int maximumDisplayOrder(@Param("outletId") String outletId);

    Optional<Category> findByOutletIdAndNameIgnoreCaseAndArchivedFalse(String outletId, String name);
}

interface ProductRepository extends JpaRepository<Product, String> {
    @EntityGraph(attributePaths = "category")
    List<Product> findAllByOutletIdAndArchivedFalseOrderByCreatedAtAscIdAsc(String outletId);
    @EntityGraph(attributePaths = "category")
    Page<Product> findAllByOutletIdAndArchivedFalseAndNameContainingIgnoreCase(String outletId, String search, Pageable pageable);

    @EntityGraph(attributePaths = "category")
    Page<Product> findAllByOutletIdAndCategoryIdAndArchivedFalseAndNameContainingIgnoreCase(
            String outletId, String categoryId, String search, Pageable pageable);
}

interface ProductImageRepository extends JpaRepository<ProductImage, String> {
    List<ProductImage> findAllByProductIdAndArchivedFalseOrderByDisplayOrderAscIdAsc(String productId);

    @EntityGraph(attributePaths = "product")
    List<ProductImage> findAllByProductIdInAndArchivedFalseOrderByProductIdAscDisplayOrderAscIdAsc(Collection<String> productIds);

    boolean existsByObjectKeyAndArchivedFalse(String objectKey);

}

interface AddonGroupRepository extends JpaRepository<AddonGroup, String> {
    List<AddonGroup> findAllByOutletIdAndArchivedFalseOrderByDisplayOrderAscNameAsc(String outletId);

    Optional<AddonGroup> findByOutletIdAndName(String outletId, String name);
}

interface AddonOptionRepository extends JpaRepository<AddonOption, String> {
    List<AddonOption> findAllByAddonGroupIdAndArchivedFalseOrderByDisplayOrderAscNameAsc(String addonGroupId);

    @EntityGraph(attributePaths = "addonGroup")
    List<AddonOption> findAllByAddonGroupIdInAndArchivedFalseOrderByAddonGroupIdAscDisplayOrderAscNameAsc(Collection<String> addonGroupIds);
}

interface ProductAddonGroupRepository extends JpaRepository<ProductAddonGroup, String> {
    @EntityGraph(attributePaths = "addonGroup")
    List<ProductAddonGroup> findAllByProductId(String productId);

    @EntityGraph(attributePaths = "addonGroup")
    List<ProductAddonGroup> findAllByProductIdAndArchivedFalse(String productId);

    @EntityGraph(attributePaths = {"product", "addonGroup"})
    List<ProductAddonGroup> findAllByProductIdInAndArchivedFalse(Collection<String> productIds);

}

interface IngredientRepository extends JpaRepository<Ingredient, String> {
    Page<Ingredient> findAllByOutletIdAndArchivedFalseAndNameContainingIgnoreCase(String outletId, String search, Pageable pageable);
}

interface RecipeComponentRepository extends JpaRepository<RecipeComponent, String> {
    @Query("select r from RecipeComponent r join fetch r.ingredient where r.product.id=:productId and r.archived=false order by r.createdAt,r.id")
    List<RecipeComponent> findAllByProductIdAndArchivedFalse(@Param("productId") String productId);

    void deleteAllByProductId(String productId);
}
