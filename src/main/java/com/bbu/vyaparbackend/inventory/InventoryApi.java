package com.bbu.vyaparbackend.inventory;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public final class InventoryApi {
    private InventoryApi() {
    }

    public record SupplierRequest(@NotNull String name, String phone, @Email String email, String gstin,
                                  String address) {
        InventoryCommands.SupplierData toCommand() {
            return new InventoryCommands.SupplierData(name, phone, email, gstin, address);
        }
    }

    public record PurchaseLine(@NotNull String ingredientId,
                               @NotNull @DecimalMin(value = "0", inclusive = false) BigDecimal quantity,
                               @NotNull @DecimalMin("0") BigDecimal unitCost) {
        InventoryCommands.PurchaseLine toCommand() {
            return new InventoryCommands.PurchaseLine(ingredientId, quantity, unitCost);
        }
    }

    public record PurchaseRequest(String supplierId, @NotNull Instant purchasedAt, String referenceNumber, String note,
                                  @NotEmpty List<@Valid PurchaseLine> items) {
        InventoryCommands.PurchaseData toCommand() {
            return new InventoryCommands.PurchaseData(supplierId, purchasedAt, referenceNumber, note,
                    items.stream().map(PurchaseLine::toCommand).toList());
        }
    }

    public record AdjustmentRequest(@NotNull String ingredientId, @NotNull InventoryCommands.AdjustmentType type,
                                    @NotNull BigDecimal quantity, String note) {
        InventoryCommands.Adjustment toCommand() {
            return new InventoryCommands.Adjustment(ingredientId, type, quantity, note);
        }
    }

    public record SupplierView(String id, String name, String phone, String email, String gstin, String address) {
    }

    public record PurchaseLineView(String ingredientId, String ingredientName, BigDecimal quantity,
                                   BigDecimal unitCost) {
    }

    public record PurchaseView(String id, PurchaseStatus status, String supplierId, Instant purchasedAt,
                               String referenceNumber, String note, List<PurchaseLineView> items) {
    }

    public record MovementView(String id, String ingredientId, MovementType type, BigDecimal quantity, String note,
                               Instant createdAt) {
    }

    public record BalanceView(String ingredientId, BigDecimal quantity) {
    }
}
