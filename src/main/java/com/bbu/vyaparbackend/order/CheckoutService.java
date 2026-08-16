package com.bbu.vyaparbackend.order;

import com.bbu.vyaparbackend.business.InvoiceSequenceService;
import com.bbu.vyaparbackend.business.Outlet;
import com.bbu.vyaparbackend.customer.CustomerService;
import com.bbu.vyaparbackend.inventory.InventoryLedgerService;
import com.bbu.vyaparbackend.payment.PaymentService;
import com.bbu.vyaparbackend.shared.ApiException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Service
public class CheckoutService {
    private final SalesOrderRepository orders;
    private final OrderQueryService queries;
    private final CustomerService customers;
    private final PaymentService payments;
    private final InventoryLedgerService inventory;
    private final InvoiceSequenceService invoices;
    private final OrderCalculator calculator;

    CheckoutService(SalesOrderRepository orders, OrderQueryService queries, CustomerService customers,
                    PaymentService payments, InventoryLedgerService inventory, InvoiceSequenceService invoices,
                    OrderCalculator calculator) {
        this.orders = orders;
        this.queries = queries;
        this.customers = customers;
        this.payments = payments;
        this.inventory = inventory;
        this.invoices = invoices;
        this.calculator = calculator;
    }

    @Transactional
    public SalesOrder checkout(Outlet outlet, String orderId, OrderCommands.Checkout command) {
        SalesOrder order = queries.get(outlet, orderId);
        if (order.getStatus() != OrderStatus.DRAFT && order.getStatus() != OrderStatus.HELD) {
            throw ApiException.conflict("Order cannot be checked out");
        }
        List<OrderItem> lines = queries.items(order.getId());
        if (lines.isEmpty()) throw ApiException.invalid("Order must contain at least one item");
        if (command.customerId() != null) {
            order.setCustomer(customers.require(outlet.getBusiness().getId(), command.customerId()));
        }
        BigDecimal tendered = command.payments().stream().map(OrderCommands.Payment::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (tendered.compareTo(order.getTotal()) > 0) {
            throw ApiException.invalid("Payments cannot exceed the order total");
        }
        BigDecimal due = calculator.money(order.getTotal().subtract(tendered));
        if (due.signum() > 0 && order.getCustomer() == null) {
            throw ApiException.invalid("A customer is required when a balance remains due");
        }
        order.setInvoiceNumber(invoices.allocate(outlet.getId()));
        order.setClosedAt(Instant.now());
        order.setStatus(OrderStatus.CLOSED);
        command.payments().forEach(payment -> payments.record(order, payment));
        payments.updateOrderState(order);
        orders.save(order);
        for (OrderItem line : lines) {
            inventory.consumeRecipe(outlet, line.getProduct(), line.getQuantity(), order.getId());
        }
        return order;
    }

    @Transactional
    public SalesOrder addPayment(Outlet outlet, String orderId, OrderCommands.Payment command) {
        SalesOrder order = queries.get(outlet, orderId);
        if (order.getStatus() != OrderStatus.CLOSED) {
            throw ApiException.conflict("Payments can only be added to closed orders");
        }
        if (order.getPaymentStatus() == PaymentStatus.PARTIALLY_REFUNDED
                || order.getPaymentStatus() == PaymentStatus.REFUNDED) {
            throw ApiException.conflict("Payments cannot be added after a refund");
        }
        if (command.amount().compareTo(order.getDueAmount()) > 0) {
            throw ApiException.invalid("Payment exceeds balance due");
        }
        payments.record(order, command);
        payments.updateOrderState(order);
        return order;
    }
}
