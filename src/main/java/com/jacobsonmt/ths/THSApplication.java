package com.jacobsonmt.ths;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class THSApplication {

    public static void main(String[] args) {
        SpringApplication.run( THSApplication.class, args);
    }
}
