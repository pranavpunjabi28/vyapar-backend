package com.bbu.vyaparbackend.order;

import com.bbu.vyaparbackend.business.Outlet;
import com.bbu.vyaparbackend.business.OrderSequenceService;
import com.bbu.vyaparbackend.catalog.CatalogQueryService;
import com.bbu.vyaparbackend.catalog.Product;
import com.bbu.vyaparbackend.customer.CustomerService;
import com.bbu.vyaparbackend.inventory.InventoryLedgerService;
import com.bbu.vyaparbackend.report.DashboardEventService;
import com.bbu.vyaparbackend.report.DashboardEventType;
import com.bbu.vyaparbackend.shared.ApiException;
import com.bbu.vyaparbackend.shared.LogMessages;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;

@Service
public class OrderCommandService {
    private static final Logger log = LoggerFactory.getLogger(OrderCommandService.class);

    private final SalesOrderRepository orders;
    private final OrderItemRepository items;
    private final OrderItemAddonRepository itemAddons;
    private final OrderQueryService queries;
    private final CatalogQueryService catalog;
    private final CustomerService customers;
    private final OrderCalculator calculator;
    private final OrderSequenceService orderNumbers;
    private final InventoryLedgerService inventory;
    private final DashboardEventService dashboardEvents;

    OrderCommandService(SalesOrderRepository orders, OrderItemRepository items,
                        OrderItemAddonRepository itemAddons, OrderQueryService queries,
                        CatalogQueryService catalog, CustomerService customers, OrderCalculator calculator,
                        OrderSequenceService orderNumbers, InventoryLedgerService inventory,
                        DashboardEventService dashboardEvents) {
        this.orders = orders;
        this.items = items;
        this.itemAddons = itemAddons;
        this.queries = queries;
        this.catalog = catalog;
        this.customers = customers;
        this.calculator = calculator;
        this.orderNumbers = orderNumbers;
        this.inventory = inventory;
        this.dashboardEvents = dashboardEvents;
    }

    @Transactional
    public SalesOrder saveDraft(Outlet outlet, String orderId, OrderCommands.Save command) {
        boolean creating = orderId == null;
        SalesOrder order = orderId == null ? new SalesOrder() : queries.get(outlet, orderId);
        if (order.getStatus() != OrderStatus.DRAFT && order.getStatus() != OrderStatus.HELD) {
            throw ApiException.conflict("Closed or cancelled orders cannot be edited");
        }
        order.setOutlet(outlet);
        if (orderId == null) order.setOrderNumber(orderNumbers.allocate(outlet.getId()));
        order.setTableReference(command.tableReference());
        order.setDiscountType(command.discountType() == null ? DiscountType.NONE : command.discountType());
        order.setDiscountValue(command.discountValue() == null ? BigDecimal.ZERO : command.discountValue());
        order.setCustomer(command.customerId() == null ? null
                : customers.require(outlet.getBusiness().getId(), command.customerId()));
        if (orderId == null) orders.save(order);
        itemAddons.deleteAllByOrderItemOrderId(order.getId());
        itemAddons.flush();
        items.deleteAllByOrderId(order.getId());
        items.flush();
        for (OrderCommands.Line line : command.items()) {
            Product product = catalog.product(outlet, line.productId());
            if (!product.isActive()) throw ApiException.invalid(product.getName() + " is inactive");
            var selectedAddons = catalog.selectedAddons(outlet, product, line.addonOptionIds());
            BigDecimal addonPrice = selectedAddons.stream().map(selection -> selection.option().getPrice())
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            OrderItem item = new OrderItem();
            item.setOrder(order);
            item.setProduct(product);
            item.setProductName(product.getName());
            item.setUnitPrice(product.getPrice());
            item.setQuantity(line.quantity());
            item.setLineTotal(calculator.money(product.getPrice().add(addonPrice).multiply(line.quantity())));
            item.setNote(line.note());
            items.save(item);
            selectedAddons.forEach(selection -> {
                OrderItemAddon addon = new OrderItemAddon();
                addon.setOrderItem(item);
                addon.setAddonGroup(selection.group());
                addon.setAddonOption(selection.option());
                addon.setGroupName(selection.group().getName());
                addon.setOptionName(selection.option().getName());
                addon.setUnitPrice(selection.option().getPrice());
                itemAddons.save(addon);
            });
        }
        calculator.calculate(order, queries.items(order.getId()));
        if (order.getStatus() != OrderStatus.DRAFT) {
            dashboardEvents.recordOrderChanged(order, DashboardEventType.ORDER_UPDATED);
        }
        SalesOrder saved = orders.save(order);
        int addonSelectionCount = command.items().stream()
                .mapToInt(line -> line.addonOptionIds() == null ? 0 : line.addonOptionIds().size()).sum();
        log.info(LogMessages.ORDER_SAVED, saved.getId(), outlet.getId(), saved.getOrderNumber(),
                creating ? "created" : "updated", command.items().size(), addonSelectionCount);
        return saved;
    }

    @Transactional
    public SalesOrder hold(Outlet outlet, String orderId) {
        SalesOrder order = queries.get(outlet, orderId);
        if (order.getStatus() != OrderStatus.DRAFT) throw ApiException.conflict("Only draft orders can be held");
        if (queries.items(order.getId()).isEmpty()) throw ApiException.invalid("Order must contain at least one item");
        order.setStatus(OrderStatus.HELD);
        dashboardEvents.recordOrderChanged(order, DashboardEventType.ORDER_SUBMITTED);
        log.info(LogMessages.ORDER_HELD, order.getId(), outlet.getId(), order.getOrderNumber());
        return order;
    }

    @Transactional
    public SalesOrder cancel(Outlet outlet, String orderId) {
        SalesOrder order = queries.get(outlet, orderId);
        OrderStatus previousStatus = order.getStatus();
        if (!OrderCancellationRules.canCancel(order, outlet, Instant.now())) {
            throw ApiException.conflict("This order can no longer be cancelled under the outlet cancellation policy");
        }
        if (order.getStatus() == OrderStatus.PREPARING) {
            inventory.reverseSale(outlet, order.getId());
        }
        order.setStatus(OrderStatus.CANCELLED);
        order.setCancelledAt(Instant.now());
        dashboardEvents.recordOrderChanged(order, DashboardEventType.ORDER_CANCELLED);
        log.info(LogMessages.ORDER_CANCELLED, order.getId(), outlet.getId(), order.getOrderNumber(), previousStatus);
        return order;
    }
}
