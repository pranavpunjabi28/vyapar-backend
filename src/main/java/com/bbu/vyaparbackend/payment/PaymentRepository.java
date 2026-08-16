package com.bbu.vyaparbackend.payment;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

interface PaymentRepository extends JpaRepository<Payment, String> {
    List<Payment> findAllByOrderIdAndArchivedFalseOrderByCreatedAtAscIdAsc(String orderId);

    List<Payment> findAllByOrderIdInAndArchivedFalseOrderByCreatedAtAscIdAsc(Collection<String> orderIds);
}
