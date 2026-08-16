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
import java.util.Set;

import static com.bbu.vyaparbackend.shared.Pageables.requireAllowedSort;

@Service
public class ReportQueryService {
    private static final Set<String> STOCK_SORTS = Set.of("id", "name", "lowStockThreshold");
    private final OrderQueryService orders;
    private final CatalogQueryService catalog;
    private final InventoryLedgerService inventory;

    public ReportQueryService(OrderQueryService orders, CatalogQueryService catalog, InventoryLedgerService inventory) {
        this.orders = orders;
        this.catalog = catalog;
        this.inventory = inventory;
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
        ZoneId zone = ZoneId.of(outlet.getTimezone());
        LocalDate today = LocalDate.now(zone);
        Instant todayStart = today.atStartOfDay(zone).toInstant();
        Instant tomorrow = today.plusDays(1).atStartOfDay(zone).toInstant();
        Instant yesterday = today.minusDays(1).atStartOfDay(zone).toInstant();
        ReportApi.Summary todaySummary = summary(outlet, todayStart, tomorrow);
        ReportApi.Summary yesterdaySummary = summary(outlet, yesterday, todayStart);
        LocalDate currentMonthStart = today.withDayOfMonth(1);
        LocalDate previousMonthStart = currentMonthStart.minusMonths(1);
        ReportApi.Summary currentMonth = summary(outlet, currentMonthStart.atStartOfDay(zone).toInstant(), tomorrow);
        ReportApi.Summary previousMonth = summary(outlet, previousMonthStart.atStartOfDay(zone).toInstant(),
                currentMonthStart.atStartOfDay(zone).toInstant());
        List<ReportApi.ItemRow> bestSellers = items(outlet, today.minusDays(6).atStartOfDay(zone).toInstant(), tomorrow,
                org.springframework.data.domain.PageRequest.of(0, 5)).getContent();
        List<ReportApi.StockRow> warnings = inventory.stockWarnings(outlet, 100).stream()
                .map(row -> new ReportApi.StockRow(row.ingredientId(), row.ingredientName(), row.unit(), row.quantity(),
                        row.lowStockThreshold(), row.quantity().signum() < 0 ? "NEGATIVE"
                        : row.quantity().signum() == 0 ? "OUT_OF_STOCK" : "LOW", java.util.Map.of()))
                .toList();
        return new ReportApi.Dashboard(todaySummary, yesterdaySummary,
                average(currentMonth, today.getDayOfMonth()), average(previousMonth, previousMonthStart.lengthOfMonth()),
                bestSellers, warnings);
    }

    private ReportApi.Summary summary(Outlet outlet, Instant from, Instant to) {
        OrderQueryService.Summary summary = orders.summary(outlet, from, to);
        return new ReportApi.Summary(summary.orders(), summary.sales());
    }

    private ReportApi.Average average(ReportApi.Summary summary, int days) {
        return new ReportApi.Average(div(summary.sales(), days), div(BigDecimal.valueOf(summary.orders()), days));
    }

    private BigDecimal div(BigDecimal value, int divisor) {
        return value.divide(BigDecimal.valueOf(divisor), 2, RoundingMode.HALF_UP);
    }
}
