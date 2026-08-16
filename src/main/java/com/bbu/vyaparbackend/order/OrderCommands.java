package com.bbu.vyaparbackend.order;

import com.bbu.vyaparbackend.payment.PaymentMethod;

import java.math.BigDecimal;
import java.util.List;

public final class OrderCommands {
    private OrderCommands() {
    }

    public record Line(String productId, BigDecimal quantity, String note) {
    }

    public record Save(String tableReference, String customerId, DiscountType discountType, BigDecimal discountValue,
                       List<Line> items) {
    }

    public record Payment(PaymentMethod method, BigDecimal amount, String customMethod, String reference) {
    }

    public record Checkout(String customerId, List<Payment> payments) {
    }

    public record RefundLine(String orderItemId, BigDecimal quantity) {
    }

    public record Refund(BigDecimal amount, boolean restoreStock, String reason, List<RefundLine> items) {
    }
}
