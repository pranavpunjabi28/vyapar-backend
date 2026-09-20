package com.bbu.vyaparbackend.order;

import com.bbu.vyaparbackend.shared.ApiException;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Component
public class OrderCalculator {
    private static final int MONEY_SCALE = 2;

    public void calculate(SalesOrder order, List<OrderItem> lines) {
        BigDecimal subtotal = lines.stream().map(OrderItem::getLineTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        if (order.getDiscountType() == DiscountType.PERCENTAGE
                && order.getDiscountValue().compareTo(BigDecimal.valueOf(100)) > 0) {
            throw ApiException.invalid("Percentage discount cannot exceed 100");
        }
        BigDecimal discount = switch (order.getDiscountType()) {
            case NONE -> BigDecimal.ZERO;
            case FIXED -> order.getDiscountValue();
            case PERCENTAGE -> subtotal.multiply(order.getDiscountValue())
                    .divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
        };
        if (discount.signum() < 0 || discount.compareTo(subtotal) > 0) {
            throw ApiException.invalid("Discount must be between zero and subtotal");
        }
        order.setSubtotal(money(subtotal));
        order.setDiscountAmount(money(discount));
        order.setTotal(money(subtotal.subtract(discount)));
        order.setDueAmount(order.getTotal());
    }

    public BigDecimal money(BigDecimal value) {
        return value.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }
}
