package com.bbu.vyaparbackend.inventory;

import com.bbu.vyaparbackend.business.Outlet;
import com.bbu.vyaparbackend.catalog.CatalogQueryService;
import com.bbu.vyaparbackend.catalog.Ingredient;
import com.bbu.vyaparbackend.catalog.Product;
import com.bbu.vyaparbackend.catalog.RecipeComponent;
import com.bbu.vyaparbackend.shared.ApiException;
import com.bbu.vyaparbackend.shared.PrefixedIdGenerator;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;

import static com.bbu.vyaparbackend.shared.Pageables.requireAllowedSort;

@Service
public class InventoryLedgerService {
    private final InventoryMovementRepository movements;
    private final CatalogQueryService catalog;

    InventoryLedgerService(InventoryMovementRepository movements, CatalogQueryService catalog) {
        this.movements = movements;
        this.catalog = catalog;
    }

    @Transactional
    public InventoryMovement adjust(Outlet outlet, InventoryCommands.Adjustment command) {
        if (command.quantity().signum() == 0) throw ApiException.invalid("Adjustment quantity cannot be zero");
        if (command.type() != InventoryCommands.AdjustmentType.CORRECTION && command.quantity().signum() < 0) {
            throw ApiException.invalid("Opening and wastage quantities must be positive");
        }
        Ingredient ingredient = catalog.ingredient(outlet, command.ingredientId());
        MovementType type = switch (command.type()) {
            case OPENING -> MovementType.OPENING;
            case WASTAGE -> MovementType.WASTAGE;
            case CORRECTION -> MovementType.CORRECTION;
        };
        BigDecimal signed = type == MovementType.WASTAGE ? command.quantity().abs().negate() : command.quantity();
        return move(outlet, ingredient, type, signed, "ADJUSTMENT", PrefixedIdGenerator.generate("adjustment"), command.note());
    }

    @Transactional(readOnly = true)
    public BigDecimal balance(Outlet outlet, String ingredientId) {
        catalog.ingredient(outlet, ingredientId);
        return movements.balance(outlet.getId(), ingredientId);
    }

    @Transactional(readOnly = true)
    public Page<InventoryMovement> movements(Outlet outlet, String ingredientId, Pageable pageable) {
        catalog.ingredient(outlet, ingredientId);
        pageable = requireAllowedSort(pageable, Set.of("id", "createdAt", "type", "quantity"),
                Sort.by(Sort.Direction.DESC, "createdAt"));
        return movements.findAllByOutletIdAndIngredientIdAndArchivedFalse(outlet.getId(), ingredientId, pageable);
    }

    @Transactional(readOnly = true)
    public Map<String, StockMetrics> stockMetrics(Outlet outlet, Collection<String> ingredientIds) {
        if (ingredientIds.isEmpty()) return Map.of();
        Map<String, BigDecimal> balances = new HashMap<>();
        for (Object[] row : movements.balances(outlet.getId(), ingredientIds)) {
            balances.put((String) row[0], (BigDecimal) row[1]);
        }
        Map<String, Map<String, BigDecimal>> totals = new HashMap<>();
        for (Object[] row : movements.totals(outlet.getId(), ingredientIds)) {
            totals.computeIfAbsent((String) row[0], ignored -> new HashMap<>())
                    .put(((MovementType) row[1]).name(), (BigDecimal) row[2]);
        }
        Map<String, StockMetrics> result = new HashMap<>();
        for (String ingredientId : ingredientIds) {
            result.put(ingredientId, new StockMetrics(balances.getOrDefault(ingredientId, BigDecimal.ZERO),
                    Map.copyOf(totals.getOrDefault(ingredientId, Map.of()))));
        }
        return result;
    }

    @Transactional(readOnly = true)
    public List<StockWarning> stockWarnings(Outlet outlet, int limit) {
        return movements.stockWarnings(outlet.getId(), org.springframework.data.domain.PageRequest.of(0, limit)).stream()
                .map(row -> new StockWarning((String) row[0], (String) row[1], (String) row[2],
                        (BigDecimal) row[3], (BigDecimal) row[4]))
                .toList();
    }

    @Transactional
    public InventoryMovement move(Outlet outlet, Ingredient ingredient, MovementType type, BigDecimal quantity,
                                  String referenceType, String referenceId, String note) {
        InventoryMovement movement = new InventoryMovement();
        movement.setOutlet(outlet);
        movement.setIngredient(ingredient);
        movement.setType(type);
        movement.setQuantity(quantity);
        movement.setReferenceType(referenceType);
        movement.setReferenceId(referenceId);
        movement.setNote(note);
        return movements.save(movement);
    }

    @Transactional
    public void consumeRecipe(Outlet outlet, Product product, BigDecimal quantity, String orderId) {
        for (RecipeComponent component : catalog.recipe(outlet, product.getId())) {
            move(outlet, component.getIngredient(), MovementType.SALE,
                    component.getQuantity().multiply(quantity).negate(), "ORDER", orderId, product.getName());
        }
    }

    @Transactional
    public void restoreRecipe(Outlet outlet, Product product, BigDecimal quantity, String refundId) {
        for (RecipeComponent component : catalog.recipe(outlet, product.getId())) {
            move(outlet, component.getIngredient(), MovementType.REFUND,
                    component.getQuantity().multiply(quantity), "REFUND", refundId, product.getName());
        }
    }

    public record StockMetrics(BigDecimal quantity, Map<String, BigDecimal> movementTotals) {
    }

    public record StockWarning(String ingredientId, String ingredientName, String unit, BigDecimal quantity,
                               BigDecimal lowStockThreshold) {
    }
}
