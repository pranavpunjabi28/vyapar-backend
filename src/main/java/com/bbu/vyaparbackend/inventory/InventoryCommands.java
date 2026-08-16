package com.bbu.vyaparbackend.inventory;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public final class InventoryCommands {
    private InventoryCommands() {
    }

    public record SupplierData(String name, String phone, String email, String gstin, String address) {
    }

    public record PurchaseLine(String ingredientId, BigDecimal quantity, BigDecimal unitCost) {
    }

    public record PurchaseData(String supplierId, Instant purchasedAt, String referenceNumber, String note,
                               List<PurchaseLine> items) {
    }

    public record Adjustment(String ingredientId, AdjustmentType type, BigDecimal quantity, String note) {
    }

    public enum AdjustmentType {
        OPENING, WASTAGE, CORRECTION
    }
}
