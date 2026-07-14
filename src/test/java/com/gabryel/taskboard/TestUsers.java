package com.gabryel.taskboard;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

public class TestUsers {

    /** Registers (ignoring 409 if already exists) and logs in; returns access token. */
    public static String registerAndLogin(MockMvc mvc, ObjectMapper om, String email) throws Exception {
        String reg = "{\"email\":\"" + email + "\",\"password\":\"password123\",\"name\":\"" + email + "\"}";
        mvc.perform(post("/auth/register").contentType("application/json").content(reg));
        String login = "{\"email\":\"" + email + "\",\"password\":\"password123\"}";
        String body = mvc.perform(post("/auth/login").contentType("application/json").content(login))
                .andReturn().getResponse().getContentAsString();
        return om.readTree(body).get("accessToken").asText();
    }
}
