package com.bbu.vyaparbackend.business;

import com.bbu.vyaparbackend.auth.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class BusinessManagementService {
    private final BusinessRepository businesses;
    private final MembershipRepository memberships;

    BusinessManagementService(BusinessRepository businesses, MembershipRepository memberships) {
        this.businesses = businesses;
        this.memberships = memberships;
    }

    @Transactional
    public Business create(User owner, BusinessCommands.BusinessData command) {
        Business business = new Business();
        apply(business, command);
        businesses.save(business);
        Membership membership = new Membership();
        membership.setBusiness(business);
        membership.setUser(owner);
        membership.setRole(Role.OWNER);
        memberships.save(membership);
        return business;
    }

    @Transactional(readOnly = true)
    public List<Membership> memberships(User user) {
        return memberships.findAllByUserIdAndArchivedFalse(user.getId());
    }

    @Transactional
    public Business update(Membership actor, BusinessCommands.BusinessData command) {
        apply(actor.getBusiness(), command);
        return businesses.save(actor.getBusiness());
    }

    @Transactional
    public void setLogo(Membership actor, String key) {
        actor.getBusiness().setLogoKey(key);
        businesses.save(actor.getBusiness());
    }

    private void apply(Business business, BusinessCommands.BusinessData command) {
        business.setName(command.name());
        business.setLegalName(command.legalName());
        business.setPhone(command.phone());
        business.setGstin(command.gstin());
        business.setFssai(command.fssai());
    }
}
