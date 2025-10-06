package com.shutu.shutuapi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class ShutuApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(ShutuApiApplication.class, args);
    }

}
