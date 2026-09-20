package com.bbu.vyaparbackend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class VyaparBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(VyaparBackendApplication.class, args);
    }

}
