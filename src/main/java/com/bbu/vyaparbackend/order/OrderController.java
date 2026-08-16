package com.bbu.vyaparbackend.order;

import com.bbu.vyaparbackend.auth.CurrentUser;
import com.bbu.vyaparbackend.auth.User;
import com.bbu.vyaparbackend.business.Outlet;
import com.bbu.vyaparbackend.business.Role;
import com.bbu.vyaparbackend.business.TenantAccess;
import com.bbu.vyaparbackend.payment.PaymentService;
import com.bbu.vyaparbackend.payment.RefundService;
import com.bbu.vyaparbackend.shared.ApiEndpoints;
import com.bbu.vyaparbackend.shared.PageResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping(ApiEndpoints.Order.ROOT)
public class OrderController {
    private final OrderQueryService queries;
    private final OrderCommandService commands;
    private final CheckoutService checkout;
    private final OrderRefundService orderRefunds;
    private final PaymentService payments;
    private final RefundService refunds;
    private final CurrentUser current;
    private final TenantAccess access;

    public OrderController(OrderQueryService queries, OrderCommandService commands, CheckoutService checkout,
                           OrderRefundService orderRefunds, PaymentService payments, RefundService refunds,
                           CurrentUser current, TenantAccess access) {
        this.queries = queries;
        this.commands = commands;
        this.checkout = checkout;
        this.orderRefunds = orderRefunds;
        this.payments = payments;
        this.refunds = refunds;
        this.current = current;
        this.access = access;
    }

    private Outlet outlet(Authentication authentication, String outletId, Role... roles) {
        return access.outlet(current.require(authentication), outletId, roles);
    }

    @GetMapping
    PageResponse<OrderApi.OrderView> list(Authentication authentication, @PathVariable String outletId,
                                          Pageable pageable) {
        Outlet outlet = outlet(authentication, outletId);
        return PageResponse.from(OrderMapper.toViews(queries.list(outlet, pageable), queries, payments));
    }

    @GetMapping(ApiEndpoints.Order.BY_ID)
    OrderApi.OrderView get(Authentication authentication, @PathVariable String outletId, @PathVariable String id) {
        Outlet outlet = outlet(authentication, outletId);
        return OrderMapper.toView(queries.get(outlet, id), queries, payments);
    }

    @PostMapping
    OrderApi.OrderView create(Authentication authentication, @PathVariable String outletId,
                              @Valid @RequestBody OrderApi.OrderRequest request) {
        Outlet outlet = outlet(authentication, outletId, Role.OWNER, Role.MANAGER, Role.CASHIER);
        return OrderMapper.toView(commands.saveDraft(outlet, null, request.toCommand()), queries, payments);
    }

    @PutMapping(ApiEndpoints.Order.BY_ID)
    OrderApi.OrderView update(Authentication authentication, @PathVariable String outletId, @PathVariable String id,
                              @Valid @RequestBody OrderApi.OrderRequest request) {
        Outlet outlet = outlet(authentication, outletId, Role.OWNER, Role.MANAGER, Role.CASHIER);
        return OrderMapper.toView(commands.saveDraft(outlet, id, request.toCommand()), queries, payments);
    }

    @PostMapping(ApiEndpoints.Order.HOLD)
    OrderApi.OrderView hold(Authentication authentication, @PathVariable String outletId, @PathVariable String id) {
        Outlet outlet = outlet(authentication, outletId, Role.OWNER, Role.MANAGER, Role.CASHIER);
        return OrderMapper.toView(commands.hold(outlet, id), queries, payments);
    }

    @PostMapping(ApiEndpoints.Order.CHECKOUT)
    OrderApi.OrderView checkout(Authentication authentication, @PathVariable String outletId, @PathVariable String id,
                                @Valid @RequestBody OrderApi.CheckoutRequest request) {
        Outlet outlet = outlet(authentication, outletId, Role.OWNER, Role.MANAGER, Role.CASHIER);
        return OrderMapper.toView(checkout.checkout(outlet, id, request.toCommand()), queries, payments);
    }

    @PostMapping(ApiEndpoints.Order.PAYMENTS)
    OrderApi.OrderView payment(Authentication authentication, @PathVariable String outletId, @PathVariable String id,
                               @Valid @RequestBody OrderApi.PaymentLine request) {
        Outlet outlet = outlet(authentication, outletId, Role.OWNER, Role.MANAGER, Role.CASHIER);
        return OrderMapper.toView(checkout.addPayment(outlet, id, request.toCommand()), queries, payments);
    }

    @PostMapping(ApiEndpoints.Order.CANCEL)
    OrderApi.OrderView cancel(Authentication authentication, @PathVariable String outletId, @PathVariable String id) {
        Outlet outlet = outlet(authentication, outletId, Role.OWNER, Role.MANAGER, Role.CASHIER);
        return OrderMapper.toView(commands.cancel(outlet, id), queries, payments);
    }

    @PostMapping(ApiEndpoints.Order.REFUNDS)
    OrderApi.RefundView refund(Authentication authentication, @PathVariable String outletId, @PathVariable String id,
                               @Valid @RequestBody OrderApi.RefundRequest request) {
        User actor = current.require(authentication);
        Outlet outlet = access.outlet(actor, outletId, Role.OWNER, Role.MANAGER);
        return OrderMapper.toView(orderRefunds.refund(outlet, id, actor, request.toCommand()), refunds);
    }
}
