package com.family.kidschores;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class KidsChoresApplication {

    public static void main(String[] args) {
        SpringApplication.run(KidsChoresApplication.class, args);
    }
}
