package com.bbu.vyaparbackend.customer;

import com.bbu.vyaparbackend.auth.CurrentUser;
import com.bbu.vyaparbackend.business.Membership;
import com.bbu.vyaparbackend.business.Role;
import com.bbu.vyaparbackend.business.TenantAccess;
import com.bbu.vyaparbackend.shared.ApiEndpoints;
import com.bbu.vyaparbackend.shared.PageResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping(ApiEndpoints.Customer.ROOT)
public class CustomerController {
    private final CustomerService customers;
    private final CurrentUser current;
    private final TenantAccess access;

    public CustomerController(CustomerService customers, CurrentUser current, TenantAccess access) {
        this.customers = customers;
        this.current = current;
        this.access = access;
    }

    @GetMapping
    PageResponse<CustomerApi.View> list(Authentication a, @PathVariable String businessId, @RequestParam(defaultValue = "") String search, Pageable p) {
        access.business(current.require(a), businessId);
        return PageResponse.from(customers.list(businessId, search, p).map(CustomerMapper::toView));
    }

    @PostMapping
    CustomerApi.View create(Authentication a, @PathVariable String businessId, @Valid @RequestBody CustomerApi.Request r) {
        Membership m = access.business(current.require(a), businessId, Role.OWNER, Role.MANAGER, Role.CASHIER);
        return CustomerMapper.toView(customers.create(m.getBusiness(), r.toCommand()));
    }

    @PutMapping(ApiEndpoints.Customer.BY_ID)
    CustomerApi.View update(Authentication a, @PathVariable String businessId, @PathVariable String id, @Valid @RequestBody CustomerApi.Request r) {
        access.business(current.require(a), businessId, Role.OWNER, Role.MANAGER, Role.CASHIER);
        return CustomerMapper.toView(customers.update(businessId, id, r.toCommand()));
    }

    @DeleteMapping(ApiEndpoints.Customer.BY_ID)
    void archive(Authentication a, @PathVariable String businessId, @PathVariable String id) {
        access.business(current.require(a), businessId, Role.OWNER, Role.MANAGER);
        customers.archive(businessId, id);
    }
}
