package com.gabryel.taskboard.auth;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    @Test
    void devDefaultSecretUnderProdProfileFailsFast() {
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("prod");

        assertThatThrownBy(() -> new JwtService(JwtService.DEV_DEFAULT_SECRET, 15, env))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("JWT_SECRET must be set in production");
    }

    @Test
    void devDefaultSecretUnderDevProfileConstructsFine() {
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("dev");

        JwtService service = new JwtService(JwtService.DEV_DEFAULT_SECRET, 15, env);
        assertThat(service.generateAccessToken(java.util.UUID.randomUUID())).isNotBlank();
    }

    @Test
    void randomSecretUnderProdProfileConstructsFine() {
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("prod");

        JwtService service = new JwtService(
                "a-totally-different-random-production-secret-value-256-bits", 15, env);
        assertThat(service.generateAccessToken(java.util.UUID.randomUUID())).isNotBlank();
    }
}
