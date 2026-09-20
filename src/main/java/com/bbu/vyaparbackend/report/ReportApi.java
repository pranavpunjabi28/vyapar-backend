package com.bbu.vyaparbackend.report;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

public final class ReportApi {
    private ReportApi() {
    }

    public record Summary(long orders, BigDecimal sales) {
    }

    public record Average(BigDecimal dailySales, BigDecimal dailyOrders) {
    }

    public record ItemRow(String productId, String productName, BigDecimal quantity, BigDecimal revenue) {
    }

    public record StockRow(String ingredientId, String ingredientName, String unit, BigDecimal quantity,
                           BigDecimal lowStockThreshold, String status, Map<String, BigDecimal> movementTotals) {
    }

    public record DailyMetrics(BigDecimal netSales, long receivedOrders, long completedOrders,
                               BigDecimal averageOrderValue, BigDecimal unpaidAmount) {
    }

    public record Comparisons(BigDecimal netSalesPercent, BigDecimal receivedOrdersPercent,
                              BigDecimal completedOrdersPercent, BigDecimal averageOrderValuePercent) {
    }

    public record RecentOrder(String orderId, long orderNumber, String reference, String status,
                              String paymentStatus, BigDecimal total, BigDecimal dueAmount, Instant createdAt) {
    }

    public record DashboardSummary(Instant generatedAt, Instant summaryUpdatedAt, boolean outletOpen,
                                   DailyMetrics today, DailyMetrics yesterday, Comparisons comparisons,
                                   List<RecentOrder> recentOrders) {
    }

    public record PeriodMetrics(BigDecimal netSales, long completedOrders, BigDecimal refunds,
                                BigDecimal dailyAverage) {
    }

    public record LifetimeMetrics(BigDecimal netSales, long completedOrders, BigDecimal refunds,
                                  BigDecimal averageOrderValue) {
    }

    public record DashboardInsights(Instant generatedAt, PeriodMetrics currentMonth, PeriodMetrics previousMonth,
                                    BigDecimal monthlyAverageChangePercent, LifetimeMetrics lifetime,
                                    List<ItemRow> bestSellers) {
    }

    public record Dashboard(DashboardSummary summary, DashboardInsights insights) {
    }

}
