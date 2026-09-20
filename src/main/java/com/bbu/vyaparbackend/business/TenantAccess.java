package com.bbu.vyaparbackend.business;

import com.bbu.vyaparbackend.auth.User;
import com.bbu.vyaparbackend.shared.ApiException;
import com.bbu.vyaparbackend.shared.RequestLogContext;
import org.springframework.stereotype.Service;

import java.util.EnumSet;

@Service
public class TenantAccess {
    private final MembershipRepository memberships;
    private final OutletAssignmentRepository assignments;
    private final OutletRepository outlets;

    TenantAccess(MembershipRepository memberships, OutletAssignmentRepository assignments, OutletRepository outlets) {
        this.memberships = memberships;
        this.assignments = assignments;
        this.outlets = outlets;
    }

    public Membership business(User user, String businessId, Role... allowed) {
        Membership membership = memberships.findByBusinessIdAndUserIdAndArchivedFalseAndBusinessArchivedFalse(businessId, user.getId()).orElseThrow(ApiException::forbidden);
        if (allowed.length > 0 && !EnumSet.of(allowed[0], allowed).contains(membership.getRole()))
            throw ApiException.forbidden();
        RequestLogContext.merchant(membership.getBusiness().getId());
        return membership;
    }

    public Outlet outlet(User user, String outletId, Role... allowed) {
        Outlet outlet = outlets.findById(outletId).filter(o -> !o.isArchived()).orElseThrow(() -> ApiException.notFound("Outlet"));
        Membership membership = business(user, outlet.getBusiness().getId(), allowed);
        if (membership.getRole() != Role.OWNER && !assignments.existsByMembershipIdAndOutletIdAndArchivedFalse(membership.getId(), outletId))
            throw ApiException.forbidden();
        RequestLogContext.outlet(outlet.getId());
        return outlet;
    }
}
