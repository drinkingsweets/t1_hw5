package org.example.t1_hw4;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class T1Hw4Application {

    public static void main(String[] args) {
        SpringApplication.run(T1Hw4Application.class, args);
    }

}
