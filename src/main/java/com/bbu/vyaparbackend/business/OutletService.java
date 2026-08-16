package com.bbu.vyaparbackend.business;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class OutletService {
    private final OutletRepository outlets;
    private final OutletAssignmentRepository assignments;

    OutletService(OutletRepository outlets, OutletAssignmentRepository assignments) {
        this.outlets = outlets;
        this.assignments = assignments;
    }

    @Transactional(readOnly = true)
    public List<Outlet> list(Membership actor) {
        if (actor.getRole() == Role.OWNER) {
            return outlets.findAllByBusinessIdAndArchivedFalse(actor.getBusiness().getId());
        }
        return assignments.findAllByMembershipIdAndArchivedFalse(actor.getId()).stream()
                .map(OutletAssignment::getOutlet).filter(outlet -> !outlet.isArchived()).toList();
    }

    @Transactional
    public Outlet create(Membership actor, BusinessCommands.OutletData command) {
        Outlet outlet = new Outlet();
        outlet.setBusiness(actor.getBusiness());
        apply(outlet, command);
        return outlets.save(outlet);
    }

    @Transactional
    public Outlet update(Outlet outlet, BusinessCommands.OutletData command) {
        apply(outlet, command);
        return outlets.save(outlet);
    }

    @Transactional
    public void archive(Outlet outlet) {
        outlet.setArchived(true);
        outlets.save(outlet);
    }

    private void apply(Outlet outlet, BusinessCommands.OutletData command) {
        outlet.setName(command.name());
        outlet.setPhone(command.phone());
        outlet.setAddress(command.address());
        outlet.setCurrency(command.currency() == null ? "INR" : command.currency());
        outlet.setTimezone(command.timezone() == null ? "Asia/Kolkata" : command.timezone());
        outlet.setUpiId(command.upiId());
        outlet.setReceiptFooter(command.receiptFooter());
    }
}
