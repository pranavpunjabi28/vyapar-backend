package com.bbu.vyaparbackend.business;

import com.bbu.vyaparbackend.shared.ApiException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
public class InvoiceSequenceService {
    private final OutletRepository outlets;

    InvoiceSequenceService(OutletRepository outlets) {
        this.outlets = outlets;
    }

    @Transactional
    public String allocate(String outletId) {
        Outlet outlet = outlets.findByIdForUpdate(outletId).orElseThrow(() -> ApiException.notFound("Outlet"));
        long value = outlet.getNextInvoiceNumber();
        outlet.setNextInvoiceNumber(value + 1);
        return "INV-%06d".formatted(value);
    }
}
