package com.bbu.vyaparbackend.business;

import com.bbu.vyaparbackend.catalog.CatalogReplicationService;
import com.bbu.vyaparbackend.shared.ApiException;
import com.bbu.vyaparbackend.shared.LogMessages;
import com.bbu.vyaparbackend.shared.RequestLogContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DateTimeException;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.Currency;
import java.util.List;

@Service
public class OutletService {
    private static final Logger log = LoggerFactory.getLogger(OutletService.class);

    private final OutletRepository outlets;
    private final OutletAssignmentRepository assignments;
    private final CatalogReplicationService catalogs;

    OutletService(OutletRepository outlets, OutletAssignmentRepository assignments,
                  CatalogReplicationService catalogs) {
        this.outlets = outlets;
        this.assignments = assignments;
        this.catalogs = catalogs;
    }

    @Transactional(readOnly = true)
    public List<Outlet> list(Membership actor) {
        if (actor.getRole() == Role.OWNER) {
            return outlets.findAllByBusinessIdAndArchivedFalseOrderByNameAscIdAsc(actor.getBusiness().getId());
        }
        return assignments.findAllByMembershipIdAndArchivedFalse(actor.getId()).stream()
                .map(OutletAssignment::getOutlet).filter(outlet -> !outlet.isArchived())
                .sorted(Comparator.comparing(Outlet::getName).thenComparing(Outlet::getId)).toList();
    }

    @Transactional
    public Outlet create(Membership actor, BusinessCommands.OutletData command) {
        Outlet source = outlets.findFirstByBusinessIdAndArchivedFalseOrderByCreatedAtAscIdAsc(
                actor.getBusiness().getId()).orElse(null);
        Outlet outlet = new Outlet();
        outlet.setBusiness(actor.getBusiness());
        apply(outlet, command);
        Outlet saved = outlets.save(outlet);
        if (source != null) catalogs.copyMenu(source, saved);
        RequestLogContext.outlet(saved.getId());
        log.info(LogMessages.OUTLET_CREATED, saved.getId(), actor.getBusiness().getId(), source != null);
        return saved;
    }

    @Transactional
    public Outlet update(Outlet outlet, BusinessCommands.OutletData command) {
        apply(outlet, command);
        Outlet saved = outlets.save(outlet);
        log.info(LogMessages.OUTLET_UPDATED, saved.getId(), saved.getBusiness().getId());
        return saved;
    }

    @Transactional
    public void archive(Outlet outlet) {
        outlet.setArchived(true);
        outlets.save(outlet);
        log.info(LogMessages.OUTLET_ARCHIVED, outlet.getId(), outlet.getBusiness().getId());
    }

    private void apply(Outlet outlet, BusinessCommands.OutletData command) {
        String currency = command.currency() == null || command.currency().isBlank() ? "INR" : command.currency();
        String timezone = command.timezone() == null || command.timezone().isBlank() ? "Asia/Kolkata" : command.timezone();
        validateCurrency(currency);
        validateTimezone(timezone);
        outlet.setName(command.name().trim());
        outlet.setPhone(optional(command.phone()));
        outlet.setAddress(optional(command.address()));
        outlet.setCurrency(currency);
        outlet.setTimezone(timezone);
        outlet.setUpiId(optional(command.upiId()));
        outlet.setReceiptFooter(optional(command.receiptFooter()));
        if (command.preparingOrderCancellationPolicy() != null) {
            outlet.setPreparingOrderCancellationPolicy(command.preparingOrderCancellationPolicy());
        }
        if (command.preparingOrderCancellationMinutes() != null) {
            outlet.setPreparingOrderCancellationMinutes(command.preparingOrderCancellationMinutes());
        }
    }

    private void validateCurrency(String currency) {
        try {
            Currency.getInstance(currency);
        } catch (IllegalArgumentException exception) {
            throw ApiException.invalid("Currency must be a valid ISO 4217 code");
        }
    }

    private void validateTimezone(String timezone) {
        try {
            ZoneId.of(timezone);
        } catch (DateTimeException exception) {
            throw ApiException.invalid("Timezone must be a valid IANA timezone");
        }
    }

    private String optional(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
