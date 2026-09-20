package com.bbu.vyaparbackend.report;

import com.bbu.vyaparbackend.order.SalesOrder;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class DashboardEventService {
    private final DashboardEventRepository events;

    DashboardEventService(DashboardEventRepository events) {
        this.events = events;
    }

    public void recordOrderChanged(SalesOrder order, DashboardEventType type) {
        long revision = order.getDashboardRevision() + 1;
        order.setDashboardRevision(revision);
        DashboardEvent event = new DashboardEvent();
        event.setOutlet(order.getOutlet());
        event.setOrder(order);
        event.setEventType(type);
        event.setOrderRevision(revision);
        event.setIdempotencyKey(order.getId() + ":" + revision);
        event.setNextAttemptAt(Instant.now());
        events.save(event);
    }
}
