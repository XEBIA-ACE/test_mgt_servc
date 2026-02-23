package com.example.usermanagement;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point for the User Management Service.
 *
 * <p>Run with: {@code mvn spring-boot:run -Dspring-boot.run.profiles=dev}
 * or via Docker Compose (see docker-compose.yml).
 */
@SpringBootApplication
public class UserManagementApplication {

    public static void main(String[] args) {
        SpringApplication.run(UserManagementApplication.class, args);
    }
}
