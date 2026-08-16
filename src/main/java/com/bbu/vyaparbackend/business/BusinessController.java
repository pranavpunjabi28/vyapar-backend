package com.bbu.vyaparbackend.business;

import com.bbu.vyaparbackend.auth.CurrentUser;
import com.bbu.vyaparbackend.shared.ApiEndpoints;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(ApiEndpoints.Business.ROOT)
public class BusinessController {
    private final BusinessManagementService businesses;
    private final OutletService outlets;
    private final StaffService staff;
    private final CurrentUser current;
    private final TenantAccess access;

    public BusinessController(BusinessManagementService businesses, OutletService outlets, StaffService staff,
                              CurrentUser current, TenantAccess access) {
        this.businesses = businesses;
        this.outlets = outlets;
        this.staff = staff;
        this.current = current;
        this.access = access;
    }

    @PostMapping(ApiEndpoints.Business.BUSINESSES)
    BusinessApi.BusinessView create(Authentication authentication,
                                    @Valid @RequestBody BusinessApi.BusinessRequest request) {
        return BusinessMapper.toView(businesses.create(current.require(authentication), request.toCommand()));
    }

    @GetMapping(ApiEndpoints.Business.BUSINESSES)
    List<BusinessApi.MembershipView> mine(Authentication authentication) {
        return businesses.memberships(current.require(authentication)).stream().map(BusinessMapper::toView).toList();
    }

    @PutMapping(ApiEndpoints.Business.BUSINESS)
    BusinessApi.BusinessView update(Authentication authentication, @PathVariable String id,
                                    @Valid @RequestBody BusinessApi.BusinessRequest request) {
        Membership actor = access.business(current.require(authentication), id, Role.OWNER);
        return BusinessMapper.toView(businesses.update(actor, request.toCommand()));
    }

    @GetMapping(ApiEndpoints.Business.OUTLETS)
    List<BusinessApi.OutletView> outlets(Authentication authentication, @PathVariable String id) {
        Membership actor = access.business(current.require(authentication), id);
        return outlets.list(actor).stream().map(BusinessMapper::toView).toList();
    }

    @PostMapping(ApiEndpoints.Business.OUTLETS)
    BusinessApi.OutletView createOutlet(Authentication authentication, @PathVariable String id,
                                        @Valid @RequestBody BusinessApi.OutletRequest request) {
        Membership actor = access.business(current.require(authentication), id, Role.OWNER);
        return BusinessMapper.toView(outlets.create(actor, request.toCommand()));
    }

    @PutMapping(ApiEndpoints.Business.OUTLET)
    BusinessApi.OutletView updateOutlet(Authentication authentication, @PathVariable String id,
                                        @Valid @RequestBody BusinessApi.OutletRequest request) {
        Outlet outlet = access.outlet(current.require(authentication), id, Role.OWNER, Role.MANAGER);
        return BusinessMapper.toView(outlets.update(outlet, request.toCommand()));
    }

    @DeleteMapping(ApiEndpoints.Business.OUTLET)
    void archive(Authentication authentication, @PathVariable String id) {
        outlets.archive(access.outlet(current.require(authentication), id, Role.OWNER));
    }

    @PostMapping(ApiEndpoints.Business.INVITATIONS)
    BusinessApi.InvitationView invite(Authentication authentication, @PathVariable String id,
                                      @Valid @RequestBody BusinessApi.InvitationRequest request) {
        Membership actor = access.business(current.require(authentication), id, Role.OWNER);
        return staff.invite(actor, request.toCommand());
    }

    @GetMapping(ApiEndpoints.Business.STAFF)
    List<BusinessApi.StaffView> staff(Authentication authentication, @PathVariable String id) {
        Membership actor = access.business(current.require(authentication), id, Role.OWNER);
        return staff.list(actor).stream().map(member -> BusinessMapper.toView(member, staff.assignments(member))).toList();
    }

    @PutMapping(ApiEndpoints.Business.STAFF_MEMBER)
    BusinessApi.StaffView staff(Authentication authentication, @PathVariable String id,
                                @PathVariable String membershipId,
                                @Valid @RequestBody BusinessApi.StaffUpdate request) {
        Membership actor = access.business(current.require(authentication), id, Role.OWNER);
        Membership member = staff.update(actor, membershipId, request.toCommand());
        return BusinessMapper.toView(member, staff.assignments(member));
    }

    @PostMapping(ApiEndpoints.Business.ACCEPT_INVITATION)
    void accept(@PathVariable String token, @Valid @RequestBody BusinessApi.AcceptInvitation request) {
        staff.accept(token, request.toCommand());
    }
}
