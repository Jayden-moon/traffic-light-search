package com.trafficlight.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.trafficlight")
public class TrafficLightApplication {
    public static void main(String[] args) {
        SpringApplication.run(TrafficLightApplication.class, args);
    }
}
