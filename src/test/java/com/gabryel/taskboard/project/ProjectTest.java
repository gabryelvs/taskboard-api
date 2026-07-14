package com.gabryel.taskboard.project;

import com.gabryel.taskboard.IntegrationTest;
import com.gabryel.taskboard.TestUsers;
import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ProjectTest extends IntegrationTest {

    private String token(String email) throws Exception {
        return TestUsers.registerAndLogin(mvc, om, email);
    }

    private String createProject(String token, String name) throws Exception {
        String body = mvc.perform(post("/projects")
                .header("Authorization", "Bearer " + token)
                .contentType("application/json")
                .content("{\"name\":\"" + name + "\",\"description\":\"d\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return om.readTree(body).get("id").asText();
    }

    @Test
    void createAndGetProject() throws Exception {
        String t = token("proj1@test.com");
        String id = createProject(t, "My Board");
        mvc.perform(get("/projects/" + id).header("Authorization", "Bearer " + t))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.name").value("My Board"));
    }

    @Test
    void listShowsOnlyMyProjects() throws Exception {
        String t1 = token("proj2a@test.com");
        String t2 = token("proj2b@test.com");
        createProject(t1, "Mine");
        mvc.perform(get("/projects").header("Authorization", "Bearer " + t2))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$[?(@.name == 'Mine')]").isEmpty());
    }

    @Test
    void nonMemberGets404NotForbidden() throws Exception {
        String t1 = token("proj3a@test.com");
        String t2 = token("proj3b@test.com");
        String id = createProject(t1, "Secret");
        mvc.perform(get("/projects/" + id).header("Authorization", "Bearer " + t2))
           .andExpect(status().isNotFound());
    }

    @Test
    void ownerCanUpdateAndDelete() throws Exception {
        String t = token("proj4@test.com");
        String id = createProject(t, "Old");
        mvc.perform(patch("/projects/" + id).header("Authorization", "Bearer " + t)
                .contentType("application/json").content("{\"name\":\"New\",\"description\":null}"))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.name").value("New"));
        mvc.perform(delete("/projects/" + id).header("Authorization", "Bearer " + t))
           .andExpect(status().isNoContent());
        mvc.perform(get("/projects/" + id).header("Authorization", "Bearer " + t))
           .andExpect(status().isNotFound());
    }

    @Test
    void blankNameIs400() throws Exception {
        String t = token("proj5@test.com");
        mvc.perform(post("/projects").header("Authorization", "Bearer " + t)
                .contentType("application/json").content("{\"name\":\"\"}"))
           .andExpect(status().isBadRequest());
    }
}
