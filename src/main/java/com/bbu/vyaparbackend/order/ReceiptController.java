package com.bbu.vyaparbackend.order;

import com.bbu.vyaparbackend.auth.CurrentUser;
import com.bbu.vyaparbackend.business.Outlet;
import com.bbu.vyaparbackend.business.TenantAccess;
import com.bbu.vyaparbackend.payment.PaymentService;
import com.bbu.vyaparbackend.shared.ApiEndpoints;
import com.bbu.vyaparbackend.shared.ApiException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping(ApiEndpoints.Order.RECEIPT_ROOT)
public class ReceiptController {
    private final OrderQueryService orders;
    private final PaymentService payments;
    private final ReceiptService receipts;
    private final CurrentUser current;
    private final TenantAccess access;

    public ReceiptController(OrderQueryService orders, PaymentService payments, ReceiptService receipts,
                             CurrentUser current, TenantAccess access) {
        this.orders = orders;
        this.payments = payments;
        this.receipts = receipts;
        this.current = current;
        this.access = access;
    }

    @GetMapping(ApiEndpoints.Order.RECEIPT)
    ReceiptApi.ReceiptView receipt(Authentication a, @PathVariable String outletId, @PathVariable String orderId) {
        Outlet o = access.outlet(current.require(a), outletId);
        SalesOrder order = closed(o, orderId);
        return new ReceiptApi.ReceiptView(o.getBusiness().getName(), o.getName(), o.getAddress(), o.getBusiness().getGstin(), o.getBusiness().getFssai(), o.getCurrency(), o.getUpiId(), o.getReceiptFooter(), OrderMapper.toView(order, o, orders, payments));
    }

    @GetMapping(value = ApiEndpoints.Order.RECEIPT_PDF, produces = MediaType.APPLICATION_PDF_VALUE)
    ResponseEntity<byte[]> pdf(Authentication a, @PathVariable String outletId, @PathVariable String orderId) {
        Outlet o = access.outlet(current.require(a), outletId);
        SalesOrder order = closed(o, orderId);
        byte[] body = receipts.pdf(o, order);
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=" + order.getInvoiceNumber() + ".pdf").body(body);
    }

    private SalesOrder closed(Outlet outlet, String orderId) {
        SalesOrder order = orders.get(outlet, orderId);
        if (order.getStatus() != OrderStatus.CLOSED)
            throw ApiException.conflict("Receipts are available only for closed orders");
        return order;
    }

}
