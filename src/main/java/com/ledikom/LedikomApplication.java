package com.ledikom;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@ComponentScan(basePackages = {
        "com.ledikom.bot",
        "org.telegram.telegrambots"
})
@EnableScheduling
public class LedikomApplication {

    public static void main(String[] args) {
        SpringApplication.run(LedikomApplication.class, args);
    }

}
