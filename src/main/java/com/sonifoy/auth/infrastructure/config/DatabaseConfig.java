package com.sonifoy.auth.infrastructure.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories;

@Configuration
@EnableR2dbcRepositories(basePackages = "com.sonifoy.auth.infrastructure.persistence")
public class DatabaseConfig {
}
