package com.example.trending;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class TrendingNewsApplication {
    public static void main(String[] args) {
        SpringApplication.run(TrendingNewsApplication.class, args);
    }
}
