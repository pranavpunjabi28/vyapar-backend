package com.bbu.vyaparbackend.customer;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;


public final class CustomerApi {
    private CustomerApi() {
    }

    public record Request(@NotBlank @Size(max = 160) String name, @Size(max = 30) String phone,
                          @Email @Size(max = 320) String email, @Size(max = 1000) String address) {
        CustomerCommand toCommand() {
            return new CustomerCommand(name, phone, email, address);
        }
    }

    public record View(String id, String name, String phone, String email, String address) {
    }
}

record CustomerCommand(String name, String phone, String email, String address) {
}
