package com.bbu.vyaparbackend.order;

import com.bbu.vyaparbackend.payment.PaymentMethod;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public final class OrderApi {
    private OrderApi() {
    }

    public record OrderLine(@NotNull String productId,
                            @NotNull @DecimalMin(value = "0", inclusive = false) BigDecimal quantity,
                            @Size(max = 500) String note,
                            @Size(max = 50) List<@NotBlank String> addonOptionIds) {
        OrderCommands.Line toCommand() {
            return new OrderCommands.Line(productId, quantity, note,
                    addonOptionIds == null ? List.of() : addonOptionIds);
        }
    }

    public record OrderRequest(@Size(max = 120) String tableReference, String customerId, DiscountType discountType,
                               @DecimalMin("0") BigDecimal discountValue,
                               @NotEmpty List<@Valid OrderLine> items) {
        OrderCommands.Save toCommand() {
            return new OrderCommands.Save(tableReference, customerId, discountType, discountValue,
                    items.stream().map(OrderLine::toCommand).toList());
        }
    }

    public record PaymentLine(@NotNull PaymentMethod method,
                              @NotNull @DecimalMin(value = "0", inclusive = false) BigDecimal amount,
                              @Size(max = 80) String customMethod, @Size(max = 160) String reference) {
        OrderCommands.Payment toCommand() {
            return new OrderCommands.Payment(method, amount, customMethod, reference);
        }
    }

    public record CheckoutRequest(String customerId, @NotNull List<@Valid PaymentLine> payments) {
        OrderCommands.Checkout toCommand() {
            return new OrderCommands.Checkout(customerId, payments.stream().map(PaymentLine::toCommand).toList());
        }
    }

    public record PrepareRequest(@NotNull @Size(max = 10) List<@Valid PaymentLine> payments) {
        OrderCommands.Prepare toCommand() {
            return new OrderCommands.Prepare(payments.stream().map(PaymentLine::toCommand).toList());
        }
    }

    public record RefundLine(@NotNull String orderItemId,
                             @NotNull @DecimalMin(value = "0", inclusive = false) BigDecimal quantity) {
        OrderCommands.RefundLine toCommand() {
            return new OrderCommands.RefundLine(orderItemId, quantity);
        }
    }

    public record RefundRequest(@NotNull @DecimalMin(value = "0", inclusive = false) BigDecimal amount,
                                boolean restoreStock, @NotBlank @Size(max = 500) String reason,
                                @NotEmpty List<@Valid RefundLine> items) {
        OrderCommands.Refund toCommand() {
            return new OrderCommands.Refund(amount, restoreStock, reason,
                    items.stream().map(RefundLine::toCommand).toList());
        }
    }

    public record OrderItemAddonView(String id, String addonGroupId, String addonOptionId, String groupName,
                                     String optionName, BigDecimal unitPrice) {
    }

    public record OrderItemView(String id, String productId, String productName, BigDecimal unitPrice,
                                BigDecimal quantity,
                                BigDecimal lineTotal, String note, List<OrderItemAddonView> addons) {
    }

    public record PaymentView(String id, PaymentMethod method, BigDecimal amount, String customMethod, String reference,
                              Instant createdAt) {
    }

    public record OrderView(String id, long orderNumber, OrderStatus status, PaymentStatus paymentStatus, String tableReference,
                            String invoiceNumber, String customerId, String customerName, String customerPhone,
                            DiscountType discountType,
                            BigDecimal discountValue,
                            BigDecimal subtotal, BigDecimal discountAmount, BigDecimal total, BigDecimal paidAmount,
                            BigDecimal dueAmount, boolean cancellable, Instant cancellationDeadline,
                            Instant createdAt, Instant preparedAt, Instant closedAt,
                            List<OrderItemView> items,
                            List<PaymentView> payments) {
    }

    public record DailySummaryView(LocalDate date, long totalOrders, long heldOrders, long preparingOrders,
                                   long completedOrders, long cancelledOrders, long unpaidOrders,
                                   BigDecimal completedSales) {
    }

    public record RefundItemView(String orderItemId, BigDecimal quantity) {
    }

    public record RefundView(String id, BigDecimal amount, boolean restoreStock, String reason, Instant createdAt,
                             List<RefundItemView> items) {
    }
}
