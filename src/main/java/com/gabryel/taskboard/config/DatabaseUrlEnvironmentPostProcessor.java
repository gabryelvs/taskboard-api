package com.gabryel.taskboard.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Lets {@code DATABASE_URL} be a Postgres connection URI
 * (e.g. what Neon hands out: {@code postgresql://user:pass@host/db?sslmode=require})
 * instead of requiring a pre-built {@code jdbc:postgresql://...} value.
 *
 * <p>When it is a {@code postgres://}/{@code postgresql://} URI, this
 * translates it into {@code spring.datasource.url}/{@code username}/
 * {@code password} via a property source placed ahead of everything else, so
 * it wins over {@code application.yml}'s placeholder. A {@code jdbc:...}
 * value (and {@code DATABASE_USER}/{@code DATABASE_PASSWORD}) keeps working
 * unchanged, since {@link DatabaseUrlParser} then reports nothing to do.
 *
 * <p>Registered via
 * {@code META-INF/spring/org.springframework.boot.env.EnvironmentPostProcessor.imports}.
 */
public class DatabaseUrlEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    private static final String PROPERTY_SOURCE_NAME = "databaseUrlOverride";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        String databaseUrl = environment.getProperty("DATABASE_URL");

        Optional<DatabaseUrlParser.ParsedDatabaseUrl> parsed = DatabaseUrlParser.parse(databaseUrl);
        if (parsed.isEmpty()) {
            return;
        }

        Map<String, Object> overrides = new LinkedHashMap<>();
        overrides.put("spring.datasource.url", parsed.get().jdbcUrl());
        if (parsed.get().username() != null) {
            overrides.put("spring.datasource.username", parsed.get().username());
        }
        if (parsed.get().password() != null) {
            overrides.put("spring.datasource.password", parsed.get().password());
        }

        environment.getPropertySources().addFirst(new MapPropertySource(PROPERTY_SOURCE_NAME, overrides));
    }

    @Override
    public int getOrder() {
        // Run after Spring Boot's own config data (application.yml) is loaded,
        // since we only need DATABASE_URL, which comes from the OS environment.
        return Ordered.HIGHEST_PRECEDENCE + 10;
    }
}
