package com.bbu.vyaparbackend.customer;

import com.bbu.vyaparbackend.business.Business;
import com.bbu.vyaparbackend.shared.ApiException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

import static com.bbu.vyaparbackend.shared.Pageables.requireAllowedSort;

@Service
public class CustomerService {
    private final CustomerRepository customers;

    public CustomerService(CustomerRepository customers) {
        this.customers = customers;
    }

    @Transactional(readOnly = true)
    public Page<Customer> list(String businessId, String search, Pageable pageable) {
        pageable = requireAllowedSort(pageable, Set.of("id", "name", "createdAt"), Sort.by("name"));
        return customers.findAllByBusinessIdAndArchivedFalseAndNameContainingIgnoreCase(
                businessId, search == null ? "" : search, pageable);
    }

    @Transactional
    Customer create(Business business, CustomerCommand command) {
        Customer customer = new Customer();
        customer.setBusiness(business);
        apply(customer, command);
        return customers.save(customer);
    }

    @Transactional
    Customer update(String businessId, String customerId, CustomerCommand command) {
        Customer customer = require(businessId, customerId);
        apply(customer, command);
        return customer;
    }

    @Transactional
    public void archive(String businessId, String customerId) {
        require(businessId, customerId).setArchived(true);
    }

    @Transactional(readOnly = true)
    public Customer require(String businessId, String customerId) {
        return customers.findById(customerId)
                .filter(customer -> !customer.isArchived()
                        && customer.getBusiness().getId().equals(businessId))
                .orElseThrow(() -> ApiException.notFound("Customer"));
    }

    private void apply(Customer customer, CustomerCommand command) {
        customer.setName(command.name());
        customer.setPhone(command.phone());
        customer.setEmail(command.email());
        customer.setAddress(command.address());
    }
}
