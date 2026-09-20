package com.bbu.vyaparbackend.report;

import com.bbu.vyaparbackend.order.OrderQueryService;
import com.bbu.vyaparbackend.order.OrderStatus;
import com.bbu.vyaparbackend.order.SalesOrder;
import com.bbu.vyaparbackend.payment.RefundService;
import com.bbu.vyaparbackend.shared.PrefixedIdGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.*;

@Component
class DashboardProjectionScheduler {
    private static final Logger log = LoggerFactory.getLogger(DashboardProjectionScheduler.class);
    private final DashboardProjectionProcessor processor;
    private final DashboardEventRepository events;
    private final int minimumEvents;
    private final Duration maximumWait;
    private final int maxBatches;

    DashboardProjectionScheduler(DashboardProjectionProcessor processor, DashboardEventRepository events,
                                 @Value("${app.dashboard.worker.minimum-events:100}") int minimumEvents,
                                 @Value("${app.dashboard.worker.maximum-wait-ms:300000}") long maximumWaitMs,
                                 @Value("${app.dashboard.worker.max-batches-per-run:20}") int maxBatches) {
        this.processor = processor;
        this.events = events;
        this.minimumEvents = minimumEvents;
        this.maximumWait = Duration.ofMillis(maximumWaitMs);
        this.maxBatches = maxBatches;
    }

    @Scheduled(fixedDelayString = "${app.dashboard.worker.trigger-check-ms:10000}")
    void dispatchWhenReady() {
        Instant now = Instant.now();
        List<DashboardEvent> pending = events.pending(now, PageRequest.of(0, minimumEvents));
        if (pending.isEmpty()) return;
        boolean fullBatch = pending.size() >= minimumEvents;
        boolean waitedLongEnough = !pending.getFirst().getCreatedAt().isAfter(now.minus(maximumWait));
        if (!fullBatch && !waitedLongEnough) return;
        try {
            for (int batch = 0; batch < maxBatches && processor.processNextBatch(); batch++) {
                // Each batch owns a separate transaction; continue while eligible work remains.
            }
        } catch (RuntimeException failure) {
            log.error("Dashboard projection batch failed; the durable events remain pending", failure);
            processor.deferFailedBatch();
        }
    }
}

@Service
class DashboardProjectionProcessor {
    private final DashboardEventRepository events;
    private final OutletOrderSummaryContributionRepository contributions;
    private final OutletDailySummaryRepository summaries;
    private final OrderQueryService orders;
    private final RefundService refunds;
    private final int batchSize;

    DashboardProjectionProcessor(DashboardEventRepository events,
                                 OutletOrderSummaryContributionRepository contributions,
                                 OutletDailySummaryRepository summaries, OrderQueryService orders,
                                 RefundService refunds,
                                 @Value("${app.dashboard.worker.batch-size:100}") int batchSize) {
        this.events = events;
        this.contributions = contributions;
        this.summaries = summaries;
        this.orders = orders;
        this.refunds = refunds;
        this.batchSize = batchSize;
    }

    @Transactional
    public boolean processNextBatch() {
        if (!events.tryWorkerLock()) return false;
        Instant now = Instant.now();
        List<DashboardEvent> batch = events.claim(now.toEpochMilli(), batchSize);
        if (batch.isEmpty()) return false;

        Map<String, List<DashboardEvent>> byOrder = new LinkedHashMap<>();
        batch.forEach(event -> byOrder.computeIfAbsent(event.getOrder().getId(), ignored -> new ArrayList<>()).add(event));
        Map<SummaryKey, ContributionValues> deltas = new LinkedHashMap<>();

        for (List<DashboardEvent> orderEvents : byOrder.values()) {
            DashboardEvent first = orderEvents.getFirst();
            SalesOrder order = orders.get(first.getOutlet(), first.getOrder().getId());
            OutletOrderSummaryContribution existing = contributions.findByOrderId(order.getId()).orElse(null);
            if (existing == null || order.getDashboardRevision() > existing.getAppliedRevision()) {
                ContributionValues current = calculate(order);
                ContributionValues previous = existing == null ? ContributionValues.zero() : ContributionValues.from(existing);
                SummaryKey key = new SummaryKey(order.getOutlet().getId(), current.businessDayStartAt());
                deltas.merge(key, current.minus(previous), ContributionValues::plus);
                saveContribution(existing, order, current);
            }
            orderEvents.forEach(event -> event.setProcessedAt(now));
        }

        deltas.forEach((key, delta) -> summaries.applyDelta(PrefixedIdGenerator.generate("daysummary"),
                now.toEpochMilli(), key.outletId(), key.businessDayStartAt().toEpochMilli(), delta.receivedOrders(),
                delta.completedOrders(), delta.cancelledOrders(), delta.grossSales(), delta.discountAmount(),
                delta.refundAmount(), delta.netSales(), delta.unpaidAmount()));
        return batch.size() == batchSize;
    }

    @Transactional
    public void deferFailedBatch() {
        if (!events.tryWorkerLock()) return;
        Instant now = Instant.now();
        List<DashboardEvent> batch = events.claim(now.toEpochMilli(), batchSize);
        for (DashboardEvent event : batch) {
            int attempts = event.getAttemptCount() + 1;
            long delaySeconds = Math.min(600, 5L << Math.min(attempts - 1, 7));
            event.setAttemptCount(attempts);
            event.setNextAttemptAt(now.plusSeconds(delaySeconds));
            event.setLastFailureCode("PROJECTION_FAILED");
        }
    }

    private ContributionValues calculate(SalesOrder order) {
        boolean received = order.getStatus() != OrderStatus.DRAFT;
        boolean completed = order.getStatus() == OrderStatus.CLOSED;
        boolean cancelled = order.getStatus() == OrderStatus.CANCELLED;
        BigDecimal refund = completed ? refunds.totalForOrder(order.getId()) : BigDecimal.ZERO;
        BigDecimal gross = completed ? order.getTotal() : BigDecimal.ZERO;
        BigDecimal discount = completed ? order.getDiscountAmount() : BigDecimal.ZERO;
        BigDecimal unpaid = received && !cancelled ? order.getDueAmount() : BigDecimal.ZERO;
        ZoneId zone = ZoneId.of(order.getOutlet().getTimezone());
        Instant businessDay = order.getCreatedAt().atZone(zone).toLocalDate().atStartOfDay(zone).toInstant();
        return new ContributionValues(businessDay, received ? 1 : 0, completed ? 1 : 0, cancelled ? 1 : 0,
                gross, discount, refund, gross.subtract(refund), unpaid);
    }

    private void saveContribution(OutletOrderSummaryContribution existing, SalesOrder order,
                                  ContributionValues values) {
        OutletOrderSummaryContribution contribution = existing == null
                ? new OutletOrderSummaryContribution() : existing;
        contribution.setOrder(order);
        contribution.setOutlet(order.getOutlet());
        contribution.setBusinessDayStartAt(values.businessDayStartAt());
        contribution.setAppliedRevision(order.getDashboardRevision());
        contribution.setReceivedOrders(values.receivedOrders());
        contribution.setCompletedOrders(values.completedOrders());
        contribution.setCancelledOrders(values.cancelledOrders());
        contribution.setGrossSales(values.grossSales());
        contribution.setDiscountAmount(values.discountAmount());
        contribution.setRefundAmount(values.refundAmount());
        contribution.setNetSales(values.netSales());
        contribution.setUnpaidAmount(values.unpaidAmount());
        contributions.save(contribution);
    }

    private record SummaryKey(String outletId, Instant businessDayStartAt) {
    }

    private record ContributionValues(Instant businessDayStartAt, long receivedOrders, long completedOrders,
                                      long cancelledOrders, BigDecimal grossSales, BigDecimal discountAmount,
                                      BigDecimal refundAmount, BigDecimal netSales, BigDecimal unpaidAmount) {
        static ContributionValues zero() {
            return new ContributionValues(Instant.EPOCH, 0, 0, 0, BigDecimal.ZERO, BigDecimal.ZERO,
                    BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
        }

        static ContributionValues from(OutletOrderSummaryContribution value) {
            return new ContributionValues(value.getBusinessDayStartAt(), value.getReceivedOrders(),
                    value.getCompletedOrders(), value.getCancelledOrders(), value.getGrossSales(),
                    value.getDiscountAmount(), value.getRefundAmount(), value.getNetSales(), value.getUnpaidAmount());
        }

        ContributionValues minus(ContributionValues other) {
            return new ContributionValues(businessDayStartAt, receivedOrders - other.receivedOrders,
                    completedOrders - other.completedOrders, cancelledOrders - other.cancelledOrders,
                    grossSales.subtract(other.grossSales), discountAmount.subtract(other.discountAmount),
                    refundAmount.subtract(other.refundAmount), netSales.subtract(other.netSales),
                    unpaidAmount.subtract(other.unpaidAmount));
        }

        ContributionValues plus(ContributionValues other) {
            return new ContributionValues(businessDayStartAt, receivedOrders + other.receivedOrders,
                    completedOrders + other.completedOrders, cancelledOrders + other.cancelledOrders,
                    grossSales.add(other.grossSales), discountAmount.add(other.discountAmount),
                    refundAmount.add(other.refundAmount), netSales.add(other.netSales),
                    unpaidAmount.add(other.unpaidAmount));
        }
    }
}
