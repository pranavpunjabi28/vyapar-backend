package com.bbu.vyaparbackend.business;

import com.bbu.vyaparbackend.shared.ApiException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderSequenceService {
    private final OutletRepository outlets;

    OrderSequenceService(OutletRepository outlets) {
        this.outlets = outlets;
    }

    @Transactional
    public long allocate(String outletId) {
        Outlet outlet = outlets.findByIdForUpdate(outletId)
                .orElseThrow(() -> ApiException.notFound("Outlet"));
        long value = outlet.getNextOrderNumber();
        outlet.setNextOrderNumber(value + 1);
        return value;
    }
}
