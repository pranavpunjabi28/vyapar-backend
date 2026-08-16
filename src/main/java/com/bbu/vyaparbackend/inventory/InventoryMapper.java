package com.bbu.vyaparbackend.inventory;

import java.util.List;

final class InventoryMapper {
    private InventoryMapper() {
    }

    static InventoryApi.SupplierView toView(Supplier supplier) {
        return new InventoryApi.SupplierView(supplier.getId(), supplier.getName(), supplier.getPhone(),
                supplier.getEmail(), supplier.getGstin(), supplier.getAddress());
    }

    static InventoryApi.PurchaseView toView(Purchase purchase, List<PurchaseItem> items) {
        return new InventoryApi.PurchaseView(purchase.getId(), purchase.getStatus(),
                purchase.getSupplier() == null ? null : purchase.getSupplier().getId(), purchase.getPurchasedAt(),
                purchase.getReferenceNumber(), purchase.getNote(), items.stream()
                .map(item -> new InventoryApi.PurchaseLineView(item.getIngredient().getId(),
                        item.getIngredient().getName(), item.getQuantity(), item.getUnitCost())).toList());
    }

    static InventoryApi.MovementView toView(InventoryMovement movement) {
        return new InventoryApi.MovementView(movement.getId(), movement.getIngredient().getId(), movement.getType(),
                movement.getQuantity(), movement.getNote(), movement.getCreatedAt());
    }
}
