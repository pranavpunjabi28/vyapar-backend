package com.bbu.vyaparbackend.report;

import com.bbu.vyaparbackend.business.Outlet;
import com.bbu.vyaparbackend.order.OrderQueryService;
import com.bbu.vyaparbackend.order.SalesOrder;
import com.bbu.vyaparbackend.shared.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.time.Duration;
import java.time.Instant;

@Service
public class CsvExportService {
    private static final int BATCH_SIZE = 500;
    private final OrderQueryService orders;
    private final ReportQueryService reports;
    private final int maxRangeDays;
    private final long maxRows;

    public CsvExportService(OrderQueryService orders, ReportQueryService reports,
                            @Value("${app.reports.max-range-days:366}") int maxRangeDays,
                            @Value("${app.reports.max-export-rows:100000}") long maxRows) {
        this.orders = orders;
        this.reports = reports;
        this.maxRangeDays = maxRangeDays;
        this.maxRows = maxRows;
    }

    public StreamingResponseBody orders(Outlet outlet, Instant from, Instant to) {
        validateRange(from, to);
        PageRequest firstRequest = PageRequest.of(0, BATCH_SIZE, Sort.by("createdAt").ascending().and(Sort.by("id")));
        Page<SalesOrder> first = orders.report(outlet, null, from, to, null, null, firstRequest);
        requireWithinLimit(first.getTotalElements());
        return output -> {
            try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(output,
                    java.nio.charset.StandardCharsets.UTF_8))) {
                writer.write("invoice,status,payment_status,created_at,total,paid,due\n");
                Page<SalesOrder> page = first;
                while (true) {
                    for (SalesOrder order : page) {
                        row(writer, order.getInvoiceNumber(), order.getStatus(), order.getPaymentStatus(),
                                order.getCreatedAt(), order.getTotal(), order.getPaidAmount(), order.getDueAmount());
                    }
                    if (!page.hasNext()) break;
                    page = orders.report(outlet, null, from, to, null, null, page.nextPageable());
                }
            }
        };
    }

    public StreamingResponseBody items(Outlet outlet, Instant from, Instant to) {
        validateRange(from, to);
        PageRequest firstRequest = PageRequest.of(0, BATCH_SIZE);
        Page<ReportApi.ItemRow> first = reports.items(outlet, from, to, firstRequest);
        requireWithinLimit(first.getTotalElements());
        return output -> {
            try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(output,
                    java.nio.charset.StandardCharsets.UTF_8))) {
                writer.write("product_id,product_name,quantity,revenue\n");
                Page<ReportApi.ItemRow> page = first;
                while (true) {
                    for (ReportApi.ItemRow item : page) {
                        row(writer, item.productId(), item.productName(), item.quantity(), item.revenue());
                    }
                    if (!page.hasNext()) break;
                    page = reports.items(outlet, from, to, page.nextPageable());
                }
            }
        };
    }

    public StreamingResponseBody stock(Outlet outlet) {
        PageRequest firstRequest = PageRequest.of(0, BATCH_SIZE, Sort.by("name"));
        Page<ReportApi.StockRow> first = reports.stock(outlet, firstRequest);
        requireWithinLimit(first.getTotalElements());
        return output -> {
            try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(output,
                    java.nio.charset.StandardCharsets.UTF_8))) {
                writer.write("ingredient_id,ingredient_name,unit,quantity,low_stock_threshold,status\n");
                Page<ReportApi.StockRow> page = first;
                while (true) {
                    for (ReportApi.StockRow stock : page) {
                        row(writer, stock.ingredientId(), stock.ingredientName(), stock.unit(), stock.quantity(),
                                stock.lowStockThreshold(), stock.status());
                    }
                    if (!page.hasNext()) break;
                    page = reports.stock(outlet, page.nextPageable());
                }
            }
        };
    }

    private void validateRange(Instant from, Instant to) {
        if (from == null || to == null || !from.isBefore(to)) {
            throw ApiException.invalid("from must be earlier than to");
        }
        if (Duration.between(from, to).compareTo(Duration.ofDays(maxRangeDays)) > 0) {
            throw ApiException.invalid("Export date range cannot exceed " + maxRangeDays + " days");
        }
    }

    private void requireWithinLimit(long rows) {
        if (rows > maxRows) throw ApiException.invalid("Export exceeds the maximum of " + maxRows + " rows");
    }

    private void row(BufferedWriter writer, Object... values) throws java.io.IOException {
        writer.write(String.join(",", java.util.Arrays.stream(values).map(this::cell).toList()));
        writer.newLine();
    }

    private String cell(Object value) {
        if (value == null) return "";
        String text = value.toString();
        if (!text.isEmpty() && "=+-@".indexOf(text.charAt(0)) >= 0) text = "'" + text;
        return '"' + text.replace("\"", "\"\"") + '"';
    }
}
