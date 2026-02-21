package com.sonifoy.auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories;

@SpringBootApplication(exclude = {
        org.springframework.boot.autoconfigure.data.redis.RedisReactiveAutoConfiguration.class })
@EnableR2dbcRepositories
public class AuthServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AuthServiceApplication.class, args);
    }

}
