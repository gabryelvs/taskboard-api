package com.gabryel.taskboard;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

public class TestBoards {

    public static String createProject(MockMvc mvc, ObjectMapper om, String token, String name) throws Exception {
        String body = mvc.perform(post("/projects")
                .header("Authorization", "Bearer " + token)
                .contentType("application/json")
                .content("{\"name\":\"" + name + "\"}"))
                .andReturn().getResponse().getContentAsString();
        return om.readTree(body).get("id").asText();
    }

    public static String createColumn(MockMvc mvc, ObjectMapper om, String token,
                                      String projectId, String name) throws Exception {
        String body = mvc.perform(post("/projects/" + projectId + "/columns")
                .header("Authorization", "Bearer " + token)
                .contentType("application/json")
                .content("{\"name\":\"" + name + "\"}"))
                .andReturn().getResponse().getContentAsString();
        return om.readTree(body).get("id").asText();
    }

    public static String createCard(MockMvc mvc, ObjectMapper om, String token,
                                    String columnId, String title) throws Exception {
        String body = mvc.perform(post("/columns/" + columnId + "/cards")
                .header("Authorization", "Bearer " + token)
                .contentType("application/json")
                .content("{\"title\":\"" + title + "\"}"))
                .andReturn().getResponse().getContentAsString();
        return om.readTree(body).get("id").asText();
    }
}
