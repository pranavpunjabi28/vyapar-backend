package com.bbu.vyaparbackend.business;

import com.bbu.vyaparbackend.auth.User;
import com.bbu.vyaparbackend.shared.LogMessages;
import com.bbu.vyaparbackend.shared.RequestLogContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Service
public class BusinessManagementService {
    private static final Logger log = LoggerFactory.getLogger(BusinessManagementService.class);

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
        RequestLogContext.merchant(business.getId());
        log.info(LogMessages.BUSINESS_CREATED, business.getId(), owner.getId());
        return business;
    }

    @Transactional(readOnly = true)
    public List<Membership> memberships(User user) {
        return memberships.findAllByUserIdAndArchivedFalseAndBusinessArchivedFalse(user.getId()).stream()
                .sorted(Comparator.comparing(Membership::getCreatedAt).thenComparing(Membership::getId))
                .toList();
    }

    @Transactional
    public Business update(Membership actor, BusinessCommands.BusinessData command) {
        apply(actor.getBusiness(), command);
        Business saved = businesses.save(actor.getBusiness());
        log.info(LogMessages.BUSINESS_UPDATED, saved.getId());
        return saved;
    }

    @Transactional
    public void setLogo(Membership actor, String key) {
        actor.getBusiness().setLogoKey(key);
        businesses.save(actor.getBusiness());
    }

    @Transactional
    public void archive(Membership actor) {
        actor.getBusiness().setArchived(true);
        businesses.save(actor.getBusiness());
        log.info(LogMessages.BUSINESS_ARCHIVED, actor.getBusiness().getId());
    }

    private void apply(Business business, BusinessCommands.BusinessData command) {
        business.setName(required(command.name()));
        business.setLegalName(optional(command.legalName()));
        business.setPhone(optional(command.phone()));
        business.setGstin(optional(command.gstin()));
        business.setFssai(optional(command.fssai()));
    }

    private String required(String value) {
        return value.trim();
    }

    private String optional(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
