package com.dawn.booking;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = {"com.dawn.booking", "com.dawn.catalog.internal", "com.dawn.common"})
@EnableScheduling
public class DawnBookingApplication {

    public static void main(String[] args) {
        SpringApplication.run(DawnBookingApplication.class, args);
    }

}
