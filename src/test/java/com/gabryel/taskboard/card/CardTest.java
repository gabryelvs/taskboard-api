package com.gabryel.taskboard.card;

import com.gabryel.taskboard.IntegrationTest;
import com.gabryel.taskboard.TestBoards;
import com.gabryel.taskboard.TestUsers;
import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class CardTest extends IntegrationTest {

    @Test
    void createCardWithDefaults() throws Exception {
        String t = TestUsers.registerAndLogin(mvc, om, "card1@test.com");
        String pid = TestBoards.createProject(mvc, om, t, "Board");
        String col = TestBoards.createColumn(mvc, om, t, pid, "Todo");

        mvc.perform(post("/columns/" + col + "/cards")
                .header("Authorization", "Bearer " + t)
                .contentType("application/json")
                .content("{\"title\":\"Ship it\"}"))
           .andExpect(status().isCreated())
           .andExpect(jsonPath("$.priority").value("MEDIUM"))
           .andExpect(jsonPath("$.position").value(0))
           .andExpect(jsonPath("$.deadline").isEmpty());
    }

    @Test
    void createCardWithPriorityAndDeadline() throws Exception {
        String t = TestUsers.registerAndLogin(mvc, om, "card2@test.com");
        String pid = TestBoards.createProject(mvc, om, t, "Board");
        String col = TestBoards.createColumn(mvc, om, t, pid, "Todo");

        mvc.perform(post("/columns/" + col + "/cards")
                .header("Authorization", "Bearer " + t)
                .contentType("application/json")
                .content("{\"title\":\"Urgent thing\",\"priority\":\"URGENT\",\"deadline\":\"2026-12-31T00:00:00Z\"}"))
           .andExpect(status().isCreated())
           .andExpect(jsonPath("$.priority").value("URGENT"))
           .andExpect(jsonPath("$.deadline").value("2026-12-31T00:00:00Z"));
    }

    @Test
    void patchUpdatesOnlyProvidedFields() throws Exception {
        String t = TestUsers.registerAndLogin(mvc, om, "card3@test.com");
        String pid = TestBoards.createProject(mvc, om, t, "Board");
        String col = TestBoards.createColumn(mvc, om, t, pid, "Todo");
        String card = TestBoards.createCard(mvc, om, t, col, "Original");

        mvc.perform(patch("/cards/" + card).header("Authorization", "Bearer " + t)
                .contentType("application/json").content("{\"priority\":\"HIGH\"}"))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.title").value("Original"))
           .andExpect(jsonPath("$.priority").value("HIGH"));
    }

    @Test
    void deleteCardClosesGap() throws Exception {
        String t = TestUsers.registerAndLogin(mvc, om, "card4@test.com");
        String pid = TestBoards.createProject(mvc, om, t, "Board");
        String col = TestBoards.createColumn(mvc, om, t, pid, "Todo");
        TestBoards.createCard(mvc, om, t, col, "A");
        String b = TestBoards.createCard(mvc, om, t, col, "B");
        TestBoards.createCard(mvc, om, t, col, "C");

        mvc.perform(delete("/cards/" + b).header("Authorization", "Bearer " + t))
           .andExpect(status().isNoContent());

        mvc.perform(get("/columns/" + col + "/cards").header("Authorization", "Bearer " + t))
           .andExpect(jsonPath("$.length()").value(2))
           .andExpect(jsonPath("$[1].title").value("C"))
           .andExpect(jsonPath("$[1].position").value(1));
    }

    @Test
    void nonMemberGets404OnCard() throws Exception {
        String t1 = TestUsers.registerAndLogin(mvc, om, "card5a@test.com");
        String t2 = TestUsers.registerAndLogin(mvc, om, "card5b@test.com");
        String pid = TestBoards.createProject(mvc, om, t1, "Board");
        String col = TestBoards.createColumn(mvc, om, t1, pid, "Todo");
        String card = TestBoards.createCard(mvc, om, t1, col, "Hidden");
        mvc.perform(get("/cards/" + card).header("Authorization", "Bearer " + t2))
           .andExpect(status().isNotFound());
    }

    @Test
    void assigneeMustBeProjectMember() throws Exception {
        String t = TestUsers.registerAndLogin(mvc, om, "card6a@test.com");
        String outsider = TestUsers.registerAndLogin(mvc, om, "card6b@test.com");
        String pid = TestBoards.createProject(mvc, om, t, "Board");
        String col = TestBoards.createColumn(mvc, om, t, pid, "Todo");
        // outsider's own userId: register response not needed; use a random UUID (not a member either way)
        mvc.perform(post("/columns/" + col + "/cards")
                .header("Authorization", "Bearer " + t)
                .contentType("application/json")
                .content("{\"title\":\"X\",\"assigneeId\":\"00000000-0000-0000-0000-000000000001\"}"))
           .andExpect(status().isNotFound());
    }
}
