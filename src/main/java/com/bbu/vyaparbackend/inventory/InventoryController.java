package com.bbu.vyaparbackend.inventory;

import com.bbu.vyaparbackend.auth.CurrentUser;
import com.bbu.vyaparbackend.business.Outlet;
import com.bbu.vyaparbackend.business.Role;
import com.bbu.vyaparbackend.business.TenantAccess;
import com.bbu.vyaparbackend.shared.ApiEndpoints;
import com.bbu.vyaparbackend.shared.PageResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping(ApiEndpoints.Inventory.ROOT)
public class InventoryController {
    private final SupplierService suppliers;
    private final PurchaseService purchases;
    private final InventoryLedgerService ledger;
    private final CurrentUser current;
    private final TenantAccess access;

    public InventoryController(SupplierService suppliers, PurchaseService purchases, InventoryLedgerService ledger,
                               CurrentUser current, TenantAccess access) {
        this.suppliers = suppliers;
        this.purchases = purchases;
        this.ledger = ledger;
        this.current = current;
        this.access = access;
    }

    private Outlet outlet(Authentication authentication, String outletId, Role... roles) {
        return access.outlet(current.require(authentication), outletId, roles);
    }

    @GetMapping(ApiEndpoints.Inventory.SUPPLIERS)
    PageResponse<InventoryApi.SupplierView> suppliers(Authentication authentication, @PathVariable String outletId,
                                                      @RequestParam(defaultValue = "") String search,
                                                      Pageable pageable) {
        return PageResponse.from(suppliers.list(outlet(authentication, outletId), search, pageable)
                .map(InventoryMapper::toView));
    }

    @PostMapping(ApiEndpoints.Inventory.SUPPLIERS)
    InventoryApi.SupplierView supplier(Authentication authentication, @PathVariable String outletId,
                                       @Valid @RequestBody InventoryApi.SupplierRequest request) {
        Outlet outlet = outlet(authentication, outletId, Role.OWNER, Role.MANAGER, Role.INVENTORY);
        return InventoryMapper.toView(suppliers.save(outlet, null, request.toCommand()));
    }

    @PutMapping(ApiEndpoints.Inventory.SUPPLIER)
    InventoryApi.SupplierView supplier(Authentication authentication, @PathVariable String outletId,
                                       @PathVariable String id,
                                       @Valid @RequestBody InventoryApi.SupplierRequest request) {
        Outlet outlet = outlet(authentication, outletId, Role.OWNER, Role.MANAGER, Role.INVENTORY);
        return InventoryMapper.toView(suppliers.save(outlet, id, request.toCommand()));
    }

    @DeleteMapping(ApiEndpoints.Inventory.SUPPLIER)
    void deleteSupplier(Authentication authentication, @PathVariable String outletId, @PathVariable String id) {
        suppliers.archive(outlet(authentication, outletId, Role.OWNER, Role.MANAGER, Role.INVENTORY), id);
    }

    @GetMapping(ApiEndpoints.Inventory.PURCHASES)
    PageResponse<InventoryApi.PurchaseView> purchases(Authentication authentication, @PathVariable String outletId,
                                                      Pageable pageable) {
        Outlet outlet = outlet(authentication, outletId);
        return PageResponse.from(purchases.list(outlet, pageable)
                .map(purchase -> InventoryMapper.toView(purchase, purchases.items(purchase))));
    }

    @PostMapping(ApiEndpoints.Inventory.PURCHASES)
    InventoryApi.PurchaseView purchase(Authentication authentication, @PathVariable String outletId,
                                       @Valid @RequestBody InventoryApi.PurchaseRequest request) {
        Outlet outlet = outlet(authentication, outletId, Role.OWNER, Role.MANAGER, Role.INVENTORY);
        Purchase purchase = purchases.save(outlet, null, request.toCommand());
        return InventoryMapper.toView(purchase, purchases.items(purchase));
    }

    @GetMapping(ApiEndpoints.Inventory.PURCHASE)
    InventoryApi.PurchaseView purchase(Authentication authentication, @PathVariable String outletId,
                                       @PathVariable String id) {
        Outlet outlet = outlet(authentication, outletId);
        Purchase purchase = purchases.require(outlet, id);
        return InventoryMapper.toView(purchase, purchases.items(purchase));
    }

    @PutMapping(ApiEndpoints.Inventory.PURCHASE)
    InventoryApi.PurchaseView purchase(Authentication authentication, @PathVariable String outletId,
                                       @PathVariable String id,
                                       @Valid @RequestBody InventoryApi.PurchaseRequest request) {
        Outlet outlet = outlet(authentication, outletId, Role.OWNER, Role.MANAGER, Role.INVENTORY);
        Purchase purchase = purchases.save(outlet, id, request.toCommand());
        return InventoryMapper.toView(purchase, purchases.items(purchase));
    }

    @PostMapping(ApiEndpoints.Inventory.POST_PURCHASE)
    void post(Authentication authentication, @PathVariable String outletId, @PathVariable String id) {
        purchases.post(outlet(authentication, outletId, Role.OWNER, Role.MANAGER, Role.INVENTORY), id);
    }

    @PostMapping(ApiEndpoints.Inventory.CANCEL_PURCHASE)
    void cancel(Authentication authentication, @PathVariable String outletId, @PathVariable String id) {
        purchases.cancel(outlet(authentication, outletId, Role.OWNER, Role.MANAGER), id);
    }

    @PostMapping(ApiEndpoints.Inventory.ADJUSTMENTS)
    InventoryApi.MovementView adjust(Authentication authentication, @PathVariable String outletId,
                                     @Valid @RequestBody InventoryApi.AdjustmentRequest request) {
        Outlet outlet = outlet(authentication, outletId, Role.OWNER, Role.MANAGER, Role.INVENTORY);
        return InventoryMapper.toView(ledger.adjust(outlet, request.toCommand()));
    }

    @GetMapping(ApiEndpoints.Inventory.BALANCE)
    InventoryApi.BalanceView balance(Authentication authentication, @PathVariable String outletId,
                                     @PathVariable String ingredientId) {
        return new InventoryApi.BalanceView(ingredientId,
                ledger.balance(outlet(authentication, outletId), ingredientId));
    }

    @GetMapping(ApiEndpoints.Inventory.MOVEMENTS)
    PageResponse<InventoryApi.MovementView> movements(Authentication authentication, @PathVariable String outletId,
                                                      @PathVariable String ingredientId, Pageable pageable) {
        return PageResponse.from(ledger.movements(outlet(authentication, outletId), ingredientId, pageable)
                .map(InventoryMapper::toView));
    }
}
