package com.bbu.vyaparbackend.catalog;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

interface CategoryRepository extends JpaRepository<Category, String> {
    List<Category> findAllByOutletIdAndArchivedFalseOrderByDisplayOrderAscNameAsc(String outletId);
}

interface ProductRepository extends JpaRepository<Product, String> {
    Page<Product> findAllByOutletIdAndArchivedFalseAndNameContainingIgnoreCase(String outletId, String search, Pageable pageable);
}

interface IngredientRepository extends JpaRepository<Ingredient, String> {
    Page<Ingredient> findAllByOutletIdAndArchivedFalseAndNameContainingIgnoreCase(String outletId, String search, Pageable pageable);
}

interface RecipeComponentRepository extends JpaRepository<RecipeComponent, String> {
    @Query("select r from RecipeComponent r join fetch r.ingredient where r.product.id=:productId and r.archived=false order by r.createdAt,r.id")
    List<RecipeComponent> findAllByProductIdAndArchivedFalse(@Param("productId") String productId);

    void deleteAllByProductId(String productId);
}
