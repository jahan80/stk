package com.starterkit.notif;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@ComponentScan(basePackages = {"com.starterkit.notif", "com.starterkit.commons"})
@EnableJpaRepositories(basePackages = "com.starterkit.notif")
@EnableScheduling
public class NotifServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(NotifServiceApplication.class, args);
    }
}
