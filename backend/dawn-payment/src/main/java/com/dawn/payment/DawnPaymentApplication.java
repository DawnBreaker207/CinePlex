package com.dawn.payment;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = {"com.dawn.payment", "com.dawn.common"})
@EnableScheduling
public class DawnPaymentApplication {

    public static void main(String[] args) {
        SpringApplication.run(DawnPaymentApplication.class, args);
    }

}
