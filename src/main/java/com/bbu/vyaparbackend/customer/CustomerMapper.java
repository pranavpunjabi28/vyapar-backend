package com.bbu.vyaparbackend.customer;

final class CustomerMapper {
    private CustomerMapper() {
    }

    static CustomerApi.View toView(Customer customer) {
        return new CustomerApi.View(customer.getId(), customer.getName(), customer.getPhone(), customer.getEmail(),
                customer.getAddress());
    }
}
