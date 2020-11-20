package com.fitmap.gateway;

import java.time.ZoneOffset;
import java.util.TimeZone;

import javax.annotation.PostConstruct;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class FitMapGatewayApplication {

    public static void main(String[] args) {

        SpringApplication.run(FitMapGatewayApplication.class, args);
    }

    @PostConstruct
    void timezoneSetup() {

        TimeZone.setDefault(TimeZone.getTimeZone(ZoneOffset.UTC));
    }

}
