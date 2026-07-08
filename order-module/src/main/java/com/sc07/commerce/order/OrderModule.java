package com.sc07.commerce.order;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class OrderModule {
    public static void main(String[] args) {
        SpringApplication.run(OrderModule.class, args);
    }
}
