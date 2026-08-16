package com.bbu.vyaparbackend.order;

import com.bbu.vyaparbackend.payment.PaymentMethod;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public final class OrderApi {
    private OrderApi() {
    }

    public record OrderLine(@NotNull String productId,
                            @NotNull @DecimalMin(value = "0", inclusive = false) BigDecimal quantity,
                            @Size(max = 500) String note) {
        OrderCommands.Line toCommand() {
            return new OrderCommands.Line(productId, quantity, note);
        }
    }

    public record OrderRequest(String tableReference, String customerId, DiscountType discountType,
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

    public record OrderItemView(String id, String productId, String productName, BigDecimal unitPrice,
                                BigDecimal quantity,
                                BigDecimal lineTotal, String note) {
    }

    public record PaymentView(String id, PaymentMethod method, BigDecimal amount, String customMethod, String reference,
                              Instant createdAt) {
    }

    public record OrderView(String id, OrderStatus status, PaymentStatus paymentStatus, String tableReference,
                            String invoiceNumber, String customerId, DiscountType discountType,
                            BigDecimal discountValue,
                            BigDecimal subtotal, BigDecimal discountAmount, BigDecimal total, BigDecimal paidAmount,
                            BigDecimal dueAmount, Instant createdAt, Instant closedAt, List<OrderItemView> items,
                            List<PaymentView> payments) {
    }

    public record RefundItemView(String orderItemId, BigDecimal quantity) {
    }

    public record RefundView(String id, BigDecimal amount, boolean restoreStock, String reason, Instant createdAt,
                             List<RefundItemView> items) {
    }
}
