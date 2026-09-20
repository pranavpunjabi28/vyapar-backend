package com.bbu.vyaparbackend.report;

import com.bbu.vyaparbackend.business.Outlet;
import com.bbu.vyaparbackend.catalog.CatalogQueryService;
import com.bbu.vyaparbackend.catalog.Ingredient;
import com.bbu.vyaparbackend.inventory.InventoryLedgerService;
import com.bbu.vyaparbackend.order.OrderQueryService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Comparator;
import java.util.Set;

import static com.bbu.vyaparbackend.shared.Pageables.requireAllowedSort;

@Service
public class ReportQueryService {
    private static final Set<String> STOCK_SORTS = Set.of("id", "name", "lowStockThreshold");
    private final OrderQueryService orders;
    private final CatalogQueryService catalog;
    private final InventoryLedgerService inventory;
    private final OutletDailySummaryRepository dailySummaries;

    public ReportQueryService(OrderQueryService orders, CatalogQueryService catalog, InventoryLedgerService inventory,
                              OutletDailySummaryRepository dailySummaries) {
        this.orders = orders;
        this.catalog = catalog;
        this.inventory = inventory;
        this.dailySummaries = dailySummaries;
    }

    @Transactional(readOnly = true)
    public Page<ReportApi.ItemRow> items(Outlet outlet, Instant from, Instant to, Pageable pageable) {
        if (pageable.getSort().isSorted()) {
            throw com.bbu.vyaparbackend.shared.ApiException.invalid("Item reports use quantity descending order");
        }
        Pageable safe = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize());
        return orders.itemReport(outlet, from, to, safe)
                .map(row -> new ReportApi.ItemRow(row.productId(), row.productName(), row.quantity(), row.revenue()));
    }

    @Transactional(readOnly = true)
    public Page<ReportApi.StockRow> stock(Outlet outlet, Pageable pageable) {
        Pageable safe = requireAllowedSort(pageable, STOCK_SORTS, Sort.by("name").ascending());
        Page<Ingredient> ingredients = catalog.ingredients(outlet, "", safe);
        List<String> ids = ingredients.getContent().stream().map(Ingredient::getId).toList();
        var metrics = inventory.stockMetrics(outlet, ids);
        return ingredients.map(ingredient -> {
            InventoryLedgerService.StockMetrics metric = metrics.get(ingredient.getId());
            BigDecimal quantity = metric.quantity();
            String status = quantity.signum() < 0 ? "NEGATIVE" : quantity.signum() == 0 ? "OUT_OF_STOCK"
                    : quantity.compareTo(ingredient.getLowStockThreshold()) <= 0 ? "LOW" : "OK";
            return new ReportApi.StockRow(ingredient.getId(), ingredient.getName(), ingredient.getUnit(), quantity,
                    ingredient.getLowStockThreshold(), status, metric.movementTotals());
        });
    }

    @Transactional(readOnly = true)
    public ReportApi.Dashboard dashboard(Outlet outlet) {
        return new ReportApi.Dashboard(dashboardSummary(outlet), dashboardInsights(outlet));
    }

    @Transactional(readOnly = true)
    public ReportApi.DashboardSummary dashboardSummary(Outlet outlet) {
        ZoneId zone = ZoneId.of(outlet.getTimezone());
        LocalDate today = LocalDate.now(zone);
        Instant todayStart = today.atStartOfDay(zone).toInstant();
        Instant tomorrow = today.plusDays(1).atStartOfDay(zone).toInstant();
        Instant yesterdayStart = today.minusDays(1).atStartOfDay(zone).toInstant();
        OutletDailySummary todayRow = dailySummaries.findByOutletIdAndBusinessDayStartAt(outlet.getId(), todayStart)
                .orElse(null);
        OutletDailySummary yesterdayRow = dailySummaries
                .findByOutletIdAndBusinessDayStartAt(outlet.getId(), yesterdayStart).orElse(null);
        ReportApi.DailyMetrics todayMetrics = dailyMetrics(todayRow);
        ReportApi.DailyMetrics yesterdayMetrics = dailyMetrics(yesterdayRow);
        List<ReportApi.RecentOrder> recent = orders.recentSubmitted(outlet, today).stream()
                .map(order -> new ReportApi.RecentOrder(order.getId(), order.getOrderNumber(),
                        order.getTableReference(), order.getStatus().name(), order.getPaymentStatus().name(),
                        order.getTotal(), order.getDueAmount(), order.getCreatedAt()))
                .toList();
        Instant updatedAt = java.util.stream.Stream.of(todayRow, yesterdayRow).filter(java.util.Objects::nonNull)
                .map(OutletDailySummary::getUpdatedAt).max(Comparator.naturalOrder()).orElse(null);
        return new ReportApi.DashboardSummary(Instant.now(), updatedAt, !outlet.isArchived(), todayMetrics,
                yesterdayMetrics, comparisons(todayMetrics, yesterdayMetrics), recent);
    }

    @Transactional(readOnly = true)
    public ReportApi.DashboardInsights dashboardInsights(Outlet outlet) {
        ZoneId zone = ZoneId.of(outlet.getTimezone());
        LocalDate today = LocalDate.now(zone);
        Instant tomorrow = today.plusDays(1).atStartOfDay(zone).toInstant();
        LocalDate currentMonthStart = today.withDayOfMonth(1);
        LocalDate previousMonthStart = currentMonthStart.minusMonths(1);
        Instant currentStart = currentMonthStart.atStartOfDay(zone).toInstant();
        Instant previousStart = previousMonthStart.atStartOfDay(zone).toInstant();
        ReportApi.PeriodMetrics currentMonth = period(dailySummaries
                .findAllByOutletIdAndBusinessDayStartAtGreaterThanEqualAndBusinessDayStartAtLessThanOrderByBusinessDayStartAt(
                        outlet.getId(), currentStart, tomorrow), today.getDayOfMonth());
        ReportApi.PeriodMetrics previousMonth = period(dailySummaries
                .findAllByOutletIdAndBusinessDayStartAtGreaterThanEqualAndBusinessDayStartAtLessThanOrderByBusinessDayStartAt(
                        outlet.getId(), previousStart, currentStart), previousMonthStart.lengthOfMonth());
        ReportApi.PeriodMetrics lifetimePeriod = period(dailySummaries.findAllByOutletId(outlet.getId()), 1);
        ReportApi.LifetimeMetrics lifetime = new ReportApi.LifetimeMetrics(lifetimePeriod.netSales(),
                lifetimePeriod.completedOrders(), lifetimePeriod.refunds(),
                averageOrderValue(lifetimePeriod.netSales(), lifetimePeriod.completedOrders()));
        List<ReportApi.ItemRow> bestSellers = items(outlet, today.minusDays(6).atStartOfDay(zone).toInstant(), tomorrow,
                org.springframework.data.domain.PageRequest.of(0, 5)).getContent();
        return new ReportApi.DashboardInsights(Instant.now(), currentMonth, previousMonth,
                percentage(currentMonth.dailyAverage(), previousMonth.dailyAverage()), lifetime, bestSellers);
    }

    private ReportApi.DailyMetrics dailyMetrics(OutletDailySummary row) {
        if (row == null) return new ReportApi.DailyMetrics(BigDecimal.ZERO, 0, 0, BigDecimal.ZERO, BigDecimal.ZERO);
        return new ReportApi.DailyMetrics(row.getNetSales(), row.getReceivedOrders(), row.getCompletedOrders(),
                averageOrderValue(row.getNetSales(), row.getCompletedOrders()), row.getUnpaidAmount());
    }

    private ReportApi.Comparisons comparisons(ReportApi.DailyMetrics today, ReportApi.DailyMetrics yesterday) {
        return new ReportApi.Comparisons(percentage(today.netSales(), yesterday.netSales()),
                percentage(BigDecimal.valueOf(today.receivedOrders()), BigDecimal.valueOf(yesterday.receivedOrders())),
                percentage(BigDecimal.valueOf(today.completedOrders()), BigDecimal.valueOf(yesterday.completedOrders())),
                percentage(today.averageOrderValue(), yesterday.averageOrderValue()));
    }

    private ReportApi.PeriodMetrics period(List<OutletDailySummary> rows, int calendarDays) {
        BigDecimal net = rows.stream().map(OutletDailySummary::getNetSales).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal refunds = rows.stream().map(OutletDailySummary::getRefundAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        long completed = rows.stream().mapToLong(OutletDailySummary::getCompletedOrders).sum();
        return new ReportApi.PeriodMetrics(net, completed, refunds, div(net, calendarDays));
    }

    private BigDecimal averageOrderValue(BigDecimal sales, long orders) {
        return orders == 0 ? BigDecimal.ZERO : sales.divide(BigDecimal.valueOf(orders), 2, RoundingMode.HALF_UP);
    }

    private BigDecimal percentage(BigDecimal current, BigDecimal previous) {
        if (previous.signum() == 0) return null;
        return current.subtract(previous).divide(previous.abs(), 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100)).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal div(BigDecimal value, int divisor) {
        return value.divide(BigDecimal.valueOf(divisor), 2, RoundingMode.HALF_UP);
    }
}
