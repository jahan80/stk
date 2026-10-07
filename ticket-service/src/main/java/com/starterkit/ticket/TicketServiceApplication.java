package com.starterkit.ticket;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

@SpringBootApplication
@ComponentScan(basePackages = {"com.starterkit.ticket", "com.starterkit.commons"})
@EntityScan(basePackages = {
        "com.starterkit.ticket",
        "com.starterkit.outbox.domain.entity"
})
@EnableJpaRepositories(basePackages = {
        "com.starterkit.ticket",
        "com.starterkit.outbox.domain.repository"
})
@EnableScheduling
@EnableMethodSecurity
public class TicketServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(TicketServiceApplication.class, args);
    }
}
