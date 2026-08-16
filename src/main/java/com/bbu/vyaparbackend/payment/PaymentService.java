package com.bbu.vyaparbackend.payment;

import com.bbu.vyaparbackend.order.OrderCalculator;
import com.bbu.vyaparbackend.order.OrderCommands;
import com.bbu.vyaparbackend.order.PaymentStatus;
import com.bbu.vyaparbackend.order.SalesOrder;
import com.bbu.vyaparbackend.shared.ApiException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class PaymentService {
    private final PaymentRepository payments;
    private final OrderCalculator calculator;

    PaymentService(PaymentRepository payments, OrderCalculator calculator) {
        this.payments = payments;
        this.calculator = calculator;
    }

    @Transactional(readOnly = true)
    public List<Payment> list(String orderId) {
        return payments.findAllByOrderIdAndArchivedFalseOrderByCreatedAtAscIdAsc(orderId);
    }

    @Transactional(readOnly = true)
    public Map<String, List<Payment>> listByOrderIds(Collection<String> orderIds) {
        if (orderIds.isEmpty()) return Map.of();
        return payments.findAllByOrderIdInAndArchivedFalseOrderByCreatedAtAscIdAsc(orderIds).stream()
                .collect(Collectors.groupingBy(payment -> payment.getOrder().getId()));
    }

    @Transactional
    public Payment record(SalesOrder order, OrderCommands.Payment command) {
        if (command.amount().signum() <= 0) throw ApiException.invalid("Payment amount must be positive");
        if (command.method() == PaymentMethod.CUSTOM
                && (command.customMethod() == null || command.customMethod().isBlank())) {
            throw ApiException.invalid("Custom payment method name is required");
        }
        Payment payment = new Payment();
        payment.setOrder(order);
        payment.setMethod(command.method());
        payment.setAmount(calculator.money(command.amount()));
        payment.setCustomMethod(command.customMethod());
        payment.setReference(command.reference());
        return payments.save(payment);
    }

    @Transactional
    public void updateOrderState(SalesOrder order) {
        BigDecimal paid = list(order.getId()).stream().map(Payment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        order.setPaidAmount(calculator.money(paid));
        order.setDueAmount(calculator.money(order.getTotal().subtract(paid)));
        order.setPaymentStatus(paid.signum() == 0 ? PaymentStatus.UNPAID
                : paid.compareTo(order.getTotal()) < 0 ? PaymentStatus.PARTIALLY_PAID : PaymentStatus.PAID);
    }
}
