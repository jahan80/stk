package com.starterkit.notif;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {"com.starterkit.notif", "com.starterkit.commons"})
public class NotifServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(NotifServiceApplication.class, args);
    }
}
