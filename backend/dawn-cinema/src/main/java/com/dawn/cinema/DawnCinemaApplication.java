package com.dawn.cinema;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {"com.dawn.cinema", "com.dawn.common"})
public class DawnCinemaApplication {

    public static void main(String[] args) {
        SpringApplication.run(DawnCinemaApplication.class, args);
    }

}
