package com.bbu.vyaparbackend.inventory;

import com.bbu.vyaparbackend.business.Outlet;
import com.bbu.vyaparbackend.catalog.CatalogQueryService;
import com.bbu.vyaparbackend.catalog.Ingredient;
import com.bbu.vyaparbackend.shared.ApiException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

import static com.bbu.vyaparbackend.shared.Pageables.requireAllowedSort;

@Service
public class PurchaseService {
    private final PurchaseRepository purchases;
    private final PurchaseItemRepository items;
    private final SupplierService suppliers;
    private final CatalogQueryService catalog;
    private final InventoryLedgerService ledger;

    PurchaseService(PurchaseRepository purchases, PurchaseItemRepository items, SupplierService suppliers,
                    CatalogQueryService catalog, InventoryLedgerService ledger) {
        this.purchases = purchases;
        this.items = items;
        this.suppliers = suppliers;
        this.catalog = catalog;
        this.ledger = ledger;
    }

    @Transactional(readOnly = true)
    public Page<Purchase> list(Outlet outlet, Pageable pageable) {
        pageable = requireAllowedSort(pageable, Set.of("id", "purchasedAt", "createdAt", "status"),
                Sort.by(Sort.Direction.DESC, "purchasedAt"));
        return purchases.findAllByOutletIdAndArchivedFalse(outlet.getId(), pageable);
    }

    @Transactional
    public Purchase save(Outlet outlet, String purchaseId, InventoryCommands.PurchaseData command) {
        Purchase purchase = purchaseId == null ? new Purchase() : require(outlet, purchaseId);
        if (purchase.getStatus() != PurchaseStatus.DRAFT) {
            throw ApiException.conflict("Only draft purchases can be edited");
        }
        purchase.setOutlet(outlet);
        purchase.setSupplier(command.supplierId() == null ? null : suppliers.require(outlet, command.supplierId()));
        purchase.setPurchasedAt(command.purchasedAt());
        purchase.setReferenceNumber(command.referenceNumber());
        purchase.setNote(command.note());
        purchases.save(purchase);
        items.deleteAllByPurchaseId(purchase.getId());
        for (InventoryCommands.PurchaseLine line : command.items()) {
            Ingredient ingredient = catalog.ingredient(outlet, line.ingredientId());
            PurchaseItem item = new PurchaseItem();
            item.setPurchase(purchase);
            item.setIngredient(ingredient);
            item.setQuantity(line.quantity());
            item.setUnitCost(line.unitCost());
            items.save(item);
        }
        return purchase;
    }

    @Transactional(readOnly = true)
    public Purchase require(Outlet outlet, String purchaseId) {
        return purchases.findById(purchaseId)
                .filter(purchase -> !purchase.isArchived() && purchase.getOutlet().getId().equals(outlet.getId()))
                .orElseThrow(() -> ApiException.notFound("Purchase"));
    }

    @Transactional(readOnly = true)
    public List<PurchaseItem> items(Purchase purchase) {
        return items.findAllByPurchaseIdAndArchivedFalse(purchase.getId());
    }

    @Transactional
    public void post(Outlet outlet, String purchaseId) {
        Purchase purchase = require(outlet, purchaseId);
        if (purchase.getStatus() != PurchaseStatus.DRAFT) throw ApiException.conflict("Purchase is not draft");
        for (PurchaseItem item : items(purchase)) {
            ledger.move(outlet, item.getIngredient(), MovementType.PURCHASE, item.getQuantity(), "PURCHASE",
                    purchase.getId(), purchase.getReferenceNumber());
        }
        purchase.setStatus(PurchaseStatus.POSTED);
    }

    @Transactional
    public void cancel(Outlet outlet, String purchaseId) {
        Purchase purchase = require(outlet, purchaseId);
        if (purchase.getStatus() != PurchaseStatus.POSTED) {
            throw ApiException.conflict("Only posted purchases can be cancelled");
        }
        for (PurchaseItem item : items(purchase)) {
            ledger.move(outlet, item.getIngredient(), MovementType.PURCHASE_REVERSAL, item.getQuantity().negate(),
                    "PURCHASE", purchase.getId(), "Purchase cancellation");
        }
        purchase.setStatus(PurchaseStatus.CANCELLED);
    }
}
