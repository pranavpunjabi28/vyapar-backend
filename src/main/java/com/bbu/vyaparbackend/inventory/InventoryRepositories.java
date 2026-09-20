package com.bbu.vyaparbackend.inventory;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;

interface SupplierRepository extends JpaRepository<Supplier, String> {
    Page<Supplier> findAllByOutletIdAndArchivedFalseAndNameContainingIgnoreCase(String outletId, String search, Pageable p);
}

interface PurchaseRepository extends JpaRepository<Purchase, String> {
    @EntityGraph(attributePaths = "supplier")
    Page<Purchase> findAllByOutletIdAndArchivedFalse(String outletId, Pageable p);

    @Override
    @EntityGraph(attributePaths = "supplier")
    java.util.Optional<Purchase> findById(String id);
}

interface PurchaseItemRepository extends JpaRepository<PurchaseItem, String> {
    @Query("select i from PurchaseItem i join fetch i.ingredient where i.purchase.id=:purchaseId and i.archived=false order by i.createdAt,i.id")
    List<PurchaseItem> findAllByPurchaseIdAndArchivedFalse(@Param("purchaseId") String purchaseId);

    void deleteAllByPurchaseId(String purchaseId);
}

interface InventoryMovementRepository extends JpaRepository<InventoryMovement, String> {
    @Query("select m from InventoryMovement m join fetch m.ingredient where m.outlet.id=:outletId and m.referenceType='ORDER' and m.referenceId=:orderId and m.type='SALE' and m.archived=false order by m.createdAt,m.id")
    List<InventoryMovement> orderSales(@Param("outletId") String outletId, @Param("orderId") String orderId);

    @Query("select coalesce(sum(m.quantity),0) from InventoryMovement m where m.outlet.id=:outletId and m.ingredient.id=:ingredientId and m.archived=false")
    BigDecimal balance(@Param("outletId") String outletId, @Param("ingredientId") String ingredientId);

    Page<InventoryMovement> findAllByOutletIdAndIngredientIdAndArchivedFalse(String outletId, String ingredientId, Pageable p);

    @Query("select m.ingredient.id,coalesce(sum(m.quantity),0) from InventoryMovement m where m.outlet.id=:outletId and m.ingredient.id in :ingredientIds and m.archived=false group by m.ingredient.id")
    List<Object[]> balances(@Param("outletId") String outletId, @Param("ingredientIds") Collection<String> ingredientIds);

    @Query("select m.ingredient.id,m.type,coalesce(sum(m.quantity),0) from InventoryMovement m where m.outlet.id=:outletId and m.ingredient.id in :ingredientIds and m.archived=false group by m.ingredient.id,m.type")
    List<Object[]> totals(@Param("outletId") String outletId, @Param("ingredientIds") Collection<String> ingredientIds);

    @Query("select i.id,i.name,i.unit,coalesce(sum(m.quantity),0),i.lowStockThreshold from Ingredient i left join InventoryMovement m on m.ingredient=i and m.archived=false where i.outlet.id=:outletId and i.archived=false group by i.id,i.name,i.unit,i.lowStockThreshold having coalesce(sum(m.quantity),0)<=i.lowStockThreshold order by coalesce(sum(m.quantity),0) asc,i.name asc")
    List<Object[]> stockWarnings(@Param("outletId") String outletId, Pageable pageable);
}
