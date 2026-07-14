package com.gabryel.taskboard.auth;

import com.gabryel.taskboard.IntegrationTest;
import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AuthTest extends IntegrationTest {

    private String body(String email) {
        return "{\"email\":\"" + email + "\",\"password\":\"password123\",\"name\":\"Gab\"}";
    }

    @Test
    void registerReturns201WithTokens() throws Exception {
        mvc.perform(post("/auth/register").contentType("application/json")
                .content(body("reg1@test.com")))
           .andExpect(status().isCreated())
           .andExpect(jsonPath("$.accessToken").isNotEmpty())
           .andExpect(jsonPath("$.refreshToken").isNotEmpty());
    }

    @Test
    void duplicateEmailReturns409() throws Exception {
        mvc.perform(post("/auth/register").contentType("application/json").content(body("dup@test.com")));
        mvc.perform(post("/auth/register").contentType("application/json").content(body("dup@test.com")))
           .andExpect(status().isConflict());
    }

    @Test
    void shortPasswordReturns400() throws Exception {
        mvc.perform(post("/auth/register").contentType("application/json")
                .content("{\"email\":\"x@test.com\",\"password\":\"short\",\"name\":\"X\"}"))
           .andExpect(status().isBadRequest());
    }

    @Test
    void loginWithCorrectPasswordReturnsTokens() throws Exception {
        mvc.perform(post("/auth/register").contentType("application/json").content(body("login1@test.com")));
        mvc.perform(post("/auth/login").contentType("application/json")
                .content("{\"email\":\"login1@test.com\",\"password\":\"password123\"}"))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.accessToken").isNotEmpty());
    }

    @Test
    void loginWithWrongPasswordReturns401() throws Exception {
        mvc.perform(post("/auth/register").contentType("application/json").content(body("login2@test.com")));
        mvc.perform(post("/auth/login").contentType("application/json")
                .content("{\"email\":\"login2@test.com\",\"password\":\"wrongpass1\"}"))
           .andExpect(status().isUnauthorized());
    }

    @Test
    void protectedEndpointWithoutTokenReturns401() throws Exception {
        mvc.perform(get("/projects")).andExpect(status().isUnauthorized());
    }

    @Test
    void protectedEndpointWithGarbageTokenReturns401() throws Exception {
        mvc.perform(get("/projects").header("Authorization", "Bearer garbage"))
           .andExpect(status().isUnauthorized());
    }
}
