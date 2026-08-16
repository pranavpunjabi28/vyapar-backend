package com.bbu.vyaparbackend.report;

import com.bbu.vyaparbackend.auth.CurrentUser;
import com.bbu.vyaparbackend.business.Outlet;
import com.bbu.vyaparbackend.business.Role;
import com.bbu.vyaparbackend.business.TenantAccess;
import com.bbu.vyaparbackend.order.OrderApi;
import com.bbu.vyaparbackend.order.OrderMapper;
import com.bbu.vyaparbackend.order.OrderQueryService;
import com.bbu.vyaparbackend.order.OrderStatus;
import com.bbu.vyaparbackend.payment.PaymentMethod;
import com.bbu.vyaparbackend.payment.PaymentService;
import com.bbu.vyaparbackend.shared.ApiEndpoints;
import com.bbu.vyaparbackend.shared.PageResponse;
import com.bbu.vyaparbackend.shared.Pageables;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.time.Instant;
import java.util.Set;

@RestController
@RequestMapping(ApiEndpoints.Report.ROOT)
public class ReportController {
    private static final Set<String> ORDER_SORTS = Set.of("id", "createdAt", "closedAt", "invoiceNumber", "status",
            "total");
    private final OrderQueryService orders;
    private final PaymentService payments;
    private final ReportQueryService reports;
    private final CsvExportService csv;
    private final CurrentUser current;
    private final TenantAccess access;

    public ReportController(OrderQueryService orders, PaymentService payments, ReportQueryService reports,
                            CsvExportService csv, CurrentUser current, TenantAccess access) {
        this.orders = orders;
        this.payments = payments;
        this.reports = reports;
        this.csv = csv;
        this.current = current;
        this.access = access;
    }

    private Outlet outlet(Authentication authentication, String outletId) {
        return access.outlet(current.require(authentication), outletId, Role.OWNER, Role.MANAGER, Role.CASHIER,
                Role.INVENTORY);
    }

    @GetMapping(ApiEndpoints.Report.ORDERS)
    PageResponse<OrderApi.OrderView> orderReport(Authentication authentication, @PathVariable String outletId,
                                                 @RequestParam(required = false) OrderStatus status,
                                                 @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
                                                 @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
                                                 @RequestParam(required = false) String customerId,
                                                 @RequestParam(required = false) PaymentMethod paymentMethod,
                                                 Pageable pageable) {
        Outlet outlet = outlet(authentication, outletId);
        Pageable safe = Pageables.requireAllowedSort(pageable, ORDER_SORTS,
                Sort.by(Sort.Direction.DESC, "createdAt"));
        return PageResponse.from(OrderMapper.toViews(
                orders.report(outlet, status, from, to, customerId, paymentMethod, safe), orders, payments));
    }

    @GetMapping(ApiEndpoints.Report.DASHBOARD)
    ReportApi.Dashboard dashboard(Authentication authentication, @PathVariable String outletId) {
        return reports.dashboard(outlet(authentication, outletId));
    }

    @GetMapping(ApiEndpoints.Report.ITEMS)
    PageResponse<ReportApi.ItemRow> items(Authentication authentication, @PathVariable String outletId,
                                          @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
                                          @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
                                          Pageable pageable) {
        return PageResponse.from(reports.items(outlet(authentication, outletId), from, to, pageable));
    }

    @GetMapping(ApiEndpoints.Report.STOCK)
    PageResponse<ReportApi.StockRow> stock(Authentication authentication, @PathVariable String outletId,
                                           Pageable pageable) {
        return PageResponse.from(reports.stock(outlet(authentication, outletId), pageable));
    }

    @GetMapping(value = ApiEndpoints.Report.ORDERS_CSV, produces = "text/csv")
    ResponseEntity<StreamingResponseBody> orderCsv(Authentication authentication, @PathVariable String outletId,
                                                   @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
                                                   @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to) {
        return csv("orders.csv", csv.orders(outlet(authentication, outletId), from, to));
    }

    @GetMapping(value = ApiEndpoints.Report.ITEMS_CSV, produces = "text/csv")
    ResponseEntity<StreamingResponseBody> itemCsv(Authentication authentication, @PathVariable String outletId,
                                                  @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
                                                  @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to) {
        return csv("items.csv", csv.items(outlet(authentication, outletId), from, to));
    }

    @GetMapping(value = ApiEndpoints.Report.STOCK_CSV, produces = "text/csv")
    ResponseEntity<StreamingResponseBody> stockCsv(Authentication authentication, @PathVariable String outletId) {
        return csv("stock.csv", csv.stock(outlet(authentication, outletId)));
    }

    private ResponseEntity<StreamingResponseBody> csv(String name, StreamingResponseBody body) {
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + name)
                .body(body);
    }
}
