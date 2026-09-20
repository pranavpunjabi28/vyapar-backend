package com.bbu.vyaparbackend.order;

import com.bbu.vyaparbackend.business.InvoiceSequenceService;
import com.bbu.vyaparbackend.business.Outlet;
import com.bbu.vyaparbackend.inventory.InventoryLedgerService;
import com.bbu.vyaparbackend.payment.PaymentService;
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
import java.util.List;

@Service
public class PreparationService {
    private static final Logger log = LoggerFactory.getLogger(PreparationService.class);

    private final SalesOrderRepository orders;
    private final OrderQueryService queries;
    private final PaymentService payments;
    private final InventoryLedgerService inventory;
    private final InvoiceSequenceService invoices;
    private final DashboardEventService dashboardEvents;

    PreparationService(SalesOrderRepository orders, OrderQueryService queries, PaymentService payments,
                       InventoryLedgerService inventory, InvoiceSequenceService invoices,
                       DashboardEventService dashboardEvents) {
        this.orders = orders;
        this.queries = queries;
        this.payments = payments;
        this.inventory = inventory;
        this.invoices = invoices;
        this.dashboardEvents = dashboardEvents;
    }

    @Transactional
    public SalesOrder prepare(Outlet outlet, String orderId, OrderCommands.Prepare command) {
        SalesOrder order = queries.get(outlet, orderId);
        if (order.getStatus() != OrderStatus.DRAFT && order.getStatus() != OrderStatus.HELD) {
            throw ApiException.conflict("Only draft or held orders can begin preparation");
        }
        List<OrderItem> lines = queries.items(order.getId());
        if (lines.isEmpty()) throw ApiException.invalid("Order must contain at least one item");

        BigDecimal tendered = command.payments().stream().map(OrderCommands.Payment::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (tendered.signum() != 0 && tendered.compareTo(order.getTotal()) != 0) {
            throw ApiException.invalid("Pay and prepare requires payment of the full order total");
        }
        if (tendered.signum() > 0) {
            order.setInvoiceNumber(invoices.allocate(outlet.getId()));
            command.payments().forEach(payment -> payments.record(order, payment));
        }
        payments.updateOrderState(order);

        order.setStatus(OrderStatus.PREPARING);
        order.setPreparedAt(Instant.now());
        for (OrderItem line : lines) {
            inventory.consumeRecipe(outlet, line.getProduct(), line.getQuantity(), order.getId());
        }
        dashboardEvents.recordOrderChanged(order, DashboardEventType.ORDER_SUBMITTED);
        SalesOrder saved = orders.save(order);
        log.info(LogMessages.ORDER_PREPARING, saved.getId(), outlet.getId(), saved.getOrderNumber(),
                tendered.signum() > 0);
        return saved;
    }
}
