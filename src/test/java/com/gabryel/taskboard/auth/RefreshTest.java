package com.gabryel.taskboard.auth;

import com.gabryel.taskboard.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.ResultActions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class RefreshTest extends IntegrationTest {

    private String register(String email) throws Exception {
        String body = mvc.perform(post("/auth/register").contentType("application/json")
                .content("{\"email\":\"" + email + "\",\"password\":\"password123\",\"name\":\"R\"}"))
                .andReturn().getResponse().getContentAsString();
        return om.readTree(body).get("refreshToken").asText();
    }

    private ResultActions refreshCall(String token) throws Exception {
        return mvc.perform(post("/auth/refresh").contentType("application/json")
                .content("{\"refreshToken\":\"" + token + "\"}"));
    }

    @Test
    void refreshReturnsNewTokenPair() throws Exception {
        String rt = register("ref1@test.com");
        String body = refreshCall(rt).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String newRt = om.readTree(body).get("refreshToken").asText();
        assertThat(newRt).isNotEqualTo(rt);
    }

    @Test
    void reusedRefreshTokenIsRejected() throws Exception {
        String rt = register("ref2@test.com");
        refreshCall(rt).andExpect(status().isOk());
        refreshCall(rt).andExpect(status().isUnauthorized()); // rotated -> old one dead
    }

    @Test
    void unknownRefreshTokenIs401() throws Exception {
        refreshCall("does-not-exist").andExpect(status().isUnauthorized());
    }

    @Test
    void logoutRevokesToken() throws Exception {
        String rt = register("ref3@test.com");
        mvc.perform(post("/auth/logout").contentType("application/json")
                .content("{\"refreshToken\":\"" + rt + "\"}"))
           .andExpect(status().isNoContent());
        refreshCall(rt).andExpect(status().isUnauthorized());
    }
}
