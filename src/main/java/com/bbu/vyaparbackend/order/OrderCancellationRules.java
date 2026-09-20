package com.bbu.vyaparbackend.order;

import com.bbu.vyaparbackend.business.OrderCancellationPolicy;
import com.bbu.vyaparbackend.business.Outlet;
import java.time.Instant;

public final class OrderCancellationRules {
    private OrderCancellationRules() {
    }

    public static boolean canCancel(SalesOrder order, Outlet outlet, Instant now) {
        if (order.getStatus() == OrderStatus.DRAFT || order.getStatus() == OrderStatus.HELD) return true;
        if (order.getStatus() != OrderStatus.PREPARING || order.getPaidAmount().signum() > 0) return false;

        return switch (outlet.getPreparingOrderCancellationPolicy()) {
            case ALWAYS -> true;
            case NEVER -> false;
            case WITHIN_WINDOW -> {
                Instant deadline = deadline(order, outlet);
                yield deadline != null && !now.isAfter(deadline);
            }
        };
    }

    public static Instant deadline(SalesOrder order, Outlet outlet) {
        if (order.getStatus() != OrderStatus.PREPARING
                || order.getPreparedAt() == null
                || outlet.getPreparingOrderCancellationPolicy() != OrderCancellationPolicy.WITHIN_WINDOW) {
            return null;
        }
        return order.getPreparedAt().plusSeconds(outlet.getPreparingOrderCancellationMinutes() * 60L);
    }
}
