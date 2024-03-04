package com.example.orange;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.TimeZone;

@SpringBootApplication
@Slf4j

public class OrangeApplication {
    @PostConstruct public void init(){ TimeZone.setDefault(TimeZone.getTimeZone("UTC+1")); }
    public static void main(String[] args) {
        SpringApplication.run(OrangeApplication.class, args);
    }

}
