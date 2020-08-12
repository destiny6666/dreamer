package com.jyq.dreamer;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class DreamerApplication {
    public static void main(String[] args) {
        SpringApplication.run(DreamerApplication.class, args);
    }
}
