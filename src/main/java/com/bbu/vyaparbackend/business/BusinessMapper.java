package com.bbu.vyaparbackend.business;

import java.util.List;

final class BusinessMapper {
    private BusinessMapper() {
    }

    static BusinessApi.BusinessView toView(Business business) {
        return new BusinessApi.BusinessView(business.getId(), business.getName(), business.getLegalName(),
                business.getPhone(), business.getGstin(), business.getFssai(), business.getLogoKey());
    }

    static BusinessApi.MembershipView toView(Membership membership) {
        return new BusinessApi.MembershipView(membership.getBusiness().getId(), membership.getBusiness().getName(),
                membership.getRole());
    }

    static BusinessApi.OutletView toView(Outlet outlet) {
        return new BusinessApi.OutletView(outlet.getId(), outlet.getBusiness().getId(), outlet.getName(),
                outlet.getPhone(), outlet.getAddress(), outlet.getCurrency(), outlet.getTimezone(), outlet.getUpiId(),
                outlet.getReceiptFooter());
    }

    static BusinessApi.StaffView toView(Membership membership, List<OutletAssignment> assignments) {
        return new BusinessApi.StaffView(membership.getId(), membership.getUser().getId(),
                membership.getUser().getDisplayName(), membership.getUser().getEmail(), membership.getRole(),
                assignments.stream().map(assignment -> assignment.getOutlet().getId()).toList());
    }
}
