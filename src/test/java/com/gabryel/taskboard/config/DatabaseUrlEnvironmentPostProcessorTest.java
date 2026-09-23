package com.gabryel.taskboard.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;

class DatabaseUrlEnvironmentPostProcessorTest {

    private final DatabaseUrlEnvironmentPostProcessor postProcessor = new DatabaseUrlEnvironmentPostProcessor();

    @Test
    void postgresUriOverridesDatasourceUrlUsernameAndPassword() {
        MockEnvironment env = new MockEnvironment();
        env.setProperty("DATABASE_URL",
                "postgresql://neondb_owner:s3cr3t@ep-cool-forest-123456.eu-west-2.aws.neon.tech/taskboard?sslmode=require&channel_binding=require");

        postProcessor.postProcessEnvironment(env, null);

        assertThat(env.getProperty("spring.datasource.url")).isEqualTo(
                "jdbc:postgresql://ep-cool-forest-123456.eu-west-2.aws.neon.tech/taskboard?sslmode=require&channel_binding=require");
        assertThat(env.getProperty("spring.datasource.username")).isEqualTo("neondb_owner");
        assertThat(env.getProperty("spring.datasource.password")).isEqualTo("s3cr3t");
    }

    @Test
    void jdbcDatabaseUrlIsLeftUntouchedForExistingPlaceholderToResolve() {
        MockEnvironment env = new MockEnvironment();
        env.setProperty("DATABASE_URL", "jdbc:postgresql://localhost:5432/taskboard");

        postProcessor.postProcessEnvironment(env, null);

        assertThat(env.getProperty("spring.datasource.url")).isNull();
        assertThat(env.getProperty("spring.datasource.username")).isNull();
        assertThat(env.getProperty("spring.datasource.password")).isNull();
    }

    @Test
    void missingDatabaseUrlIsANoOp() {
        MockEnvironment env = new MockEnvironment();

        postProcessor.postProcessEnvironment(env, null);

        assertThat(env.getProperty("spring.datasource.url")).isNull();
    }

    @Test
    void missingPasswordDoesNotOverrideDatasourcePassword() {
        MockEnvironment env = new MockEnvironment();
        env.setProperty("DATABASE_URL", "postgresql://user@db.example.com/taskboard");
        env.setProperty("spring.datasource.password", "fallback-from-DATABASE_PASSWORD");

        postProcessor.postProcessEnvironment(env, null);

        assertThat(env.getProperty("spring.datasource.username")).isEqualTo("user");
        assertThat(env.getProperty("spring.datasource.password")).isEqualTo("fallback-from-DATABASE_PASSWORD");
    }
}
