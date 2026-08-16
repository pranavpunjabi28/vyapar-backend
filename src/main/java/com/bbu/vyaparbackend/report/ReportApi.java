package com.bbu.vyaparbackend.report;

import java.math.BigDecimal;
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

    public record Dashboard(Summary today, Summary yesterday, Average currentMonth, Average previousMonth,
                            List<ItemRow> bestSellers, List<StockRow> stockWarnings) {
    }

}
