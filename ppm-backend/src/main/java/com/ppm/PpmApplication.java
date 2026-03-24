package com.ppm;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;

@SpringBootApplication(exclude = {
        SecurityAutoConfiguration.class     // 排除安全
})
public class PpmApplication {

    public static void main(String[] args) {

        SpringApplication.run(PpmApplication.class, args);
    }
}
