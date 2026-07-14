package com.gabryel.taskboard.comment;

import com.gabryel.taskboard.IntegrationTest;
import com.gabryel.taskboard.TestBoards;
import com.gabryel.taskboard.TestUsers;
import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class CommentTest extends IntegrationTest {

    private String setupCard(String token) throws Exception {
        String pid = TestBoards.createProject(mvc, om, token, "Board");
        String col = TestBoards.createColumn(mvc, om, token, pid, "Todo");
        return TestBoards.createCard(mvc, om, token, col, "Card");
    }

    private String comment(String token, String cardId, String body) throws Exception {
        String resp = mvc.perform(post("/cards/" + cardId + "/comments")
                .header("Authorization", "Bearer " + token)
                .contentType("application/json").content("{\"body\":\"" + body + "\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return om.readTree(resp).get("id").asText();
    }

    @Test
    void createAndListComments() throws Exception {
        String t = TestUsers.registerAndLogin(mvc, om, "com1@test.com");
        String card = setupCard(t);
        comment(t, card, "first");
        comment(t, card, "second");

        mvc.perform(get("/cards/" + card + "/comments").header("Authorization", "Bearer " + t))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.length()").value(2))
           .andExpect(jsonPath("$[0].body").value("first"))
           .andExpect(jsonPath("$[0].authorName").isNotEmpty());
    }

    @Test
    void authorCanEditOwnComment() throws Exception {
        String t = TestUsers.registerAndLogin(mvc, om, "com2@test.com");
        String card = setupCard(t);
        String id = comment(t, card, "typo");
        mvc.perform(patch("/comments/" + id).header("Authorization", "Bearer " + t)
                .contentType("application/json").content("{\"body\":\"fixed\"}"))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.body").value("fixed"));
    }

    @Test
    void otherMemberCannotEditComment() throws Exception {
        String owner = TestUsers.registerAndLogin(mvc, om, "com3a@test.com");
        String member = TestUsers.registerAndLogin(mvc, om, "com3b@test.com");
        String pid = TestBoards.createProject(mvc, om, owner, "Board");
        mvc.perform(post("/projects/" + pid + "/members").header("Authorization", "Bearer " + owner)
                .contentType("application/json").content("{\"email\":\"com3b@test.com\"}"));
        String col = TestBoards.createColumn(mvc, om, owner, pid, "Todo");
        String card = TestBoards.createCard(mvc, om, owner, col, "Card");
        String id = comment(owner, card, "mine");

        mvc.perform(patch("/comments/" + id).header("Authorization", "Bearer " + member)
                .contentType("application/json").content("{\"body\":\"hijack\"}"))
           .andExpect(status().isForbidden());
        mvc.perform(delete("/comments/" + id).header("Authorization", "Bearer " + member))
           .andExpect(status().isForbidden());
    }

    @Test
    void nonMemberGets404OnComments() throws Exception {
        String t1 = TestUsers.registerAndLogin(mvc, om, "com4a@test.com");
        String t2 = TestUsers.registerAndLogin(mvc, om, "com4b@test.com");
        String card = setupCard(t1);
        mvc.perform(get("/cards/" + card + "/comments").header("Authorization", "Bearer " + t2))
           .andExpect(status().isNotFound());
    }
}
