package com.bbu.vyaparbackend.customer;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;


public interface CustomerRepository extends JpaRepository<Customer, String> {
    Page<Customer> findAllByBusinessIdAndArchivedFalseAndNameContainingIgnoreCase(String businessId, String search, Pageable pageable);
}
