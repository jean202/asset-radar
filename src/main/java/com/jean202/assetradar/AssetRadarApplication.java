package com.jean202.assetradar;

import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@ConfigurationPropertiesScan
public class AssetRadarApplication {
    public static void main(String[] args) {
        SpringApplication.run(AssetRadarApplication.class, args);
    }
}
