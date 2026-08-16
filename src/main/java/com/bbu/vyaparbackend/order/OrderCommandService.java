package com.bbu.vyaparbackend.order;

import com.bbu.vyaparbackend.business.Outlet;
import com.bbu.vyaparbackend.catalog.CatalogQueryService;
import com.bbu.vyaparbackend.catalog.Product;
import com.bbu.vyaparbackend.customer.CustomerService;
import com.bbu.vyaparbackend.shared.ApiException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;

@Service
public class OrderCommandService {
    private final SalesOrderRepository orders;
    private final OrderItemRepository items;
    private final OrderQueryService queries;
    private final CatalogQueryService catalog;
    private final CustomerService customers;
    private final OrderCalculator calculator;

    OrderCommandService(SalesOrderRepository orders, OrderItemRepository items, OrderQueryService queries,
                        CatalogQueryService catalog, CustomerService customers, OrderCalculator calculator) {
        this.orders = orders;
        this.items = items;
        this.queries = queries;
        this.catalog = catalog;
        this.customers = customers;
        this.calculator = calculator;
    }

    @Transactional
    public SalesOrder saveDraft(Outlet outlet, String orderId, OrderCommands.Save command) {
        SalesOrder order = orderId == null ? new SalesOrder() : queries.get(outlet, orderId);
        if (order.getStatus() != OrderStatus.DRAFT && order.getStatus() != OrderStatus.HELD) {
            throw ApiException.conflict("Closed or cancelled orders cannot be edited");
        }
        order.setOutlet(outlet);
        order.setTableReference(command.tableReference());
        order.setDiscountType(command.discountType() == null ? DiscountType.NONE : command.discountType());
        order.setDiscountValue(command.discountValue() == null ? BigDecimal.ZERO : command.discountValue());
        order.setCustomer(command.customerId() == null ? null
                : customers.require(outlet.getBusiness().getId(), command.customerId()));
        if (orderId == null) orders.save(order);
        items.deleteAllByOrderId(order.getId());
        for (OrderCommands.Line line : command.items()) {
            Product product = catalog.product(outlet, line.productId());
            if (!product.isActive()) throw ApiException.invalid(product.getName() + " is inactive");
            OrderItem item = new OrderItem();
            item.setOrder(order);
            item.setProduct(product);
            item.setProductName(product.getName());
            item.setUnitPrice(product.getPrice());
            item.setQuantity(line.quantity());
            item.setLineTotal(calculator.money(product.getPrice().multiply(line.quantity())));
            item.setNote(line.note());
            items.save(item);
        }
        calculator.calculate(order, queries.items(order.getId()));
        return orders.save(order);
    }

    @Transactional
    public SalesOrder hold(Outlet outlet, String orderId) {
        SalesOrder order = queries.get(outlet, orderId);
        if (order.getStatus() != OrderStatus.DRAFT) throw ApiException.conflict("Only draft orders can be held");
        if (queries.items(order.getId()).isEmpty()) throw ApiException.invalid("Order must contain at least one item");
        order.setStatus(OrderStatus.HELD);
        return order;
    }

    @Transactional
    public SalesOrder cancel(Outlet outlet, String orderId) {
        SalesOrder order = queries.get(outlet, orderId);
        if (order.getStatus() != OrderStatus.DRAFT && order.getStatus() != OrderStatus.HELD) {
            throw ApiException.conflict("Closed orders must be refunded instead");
        }
        order.setStatus(OrderStatus.CANCELLED);
        order.setCancelledAt(Instant.now());
        return order;
    }
}
