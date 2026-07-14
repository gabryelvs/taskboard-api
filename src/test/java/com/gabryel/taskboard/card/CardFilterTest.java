package com.gabryel.taskboard.card;

import com.gabryel.taskboard.IntegrationTest;
import com.gabryel.taskboard.TestBoards;
import com.gabryel.taskboard.TestUsers;
import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class CardFilterTest extends IntegrationTest {

    @Test
    void filtersByPriorityAndDeadlineAcrossColumns() throws Exception {
        String t = TestUsers.registerAndLogin(mvc, om, "filter1@test.com");
        String pid = TestBoards.createProject(mvc, om, t, "Board");
        String todo = TestBoards.createColumn(mvc, om, t, pid, "Todo");
        String done = TestBoards.createColumn(mvc, om, t, pid, "Done");

        mvc.perform(post("/columns/" + todo + "/cards").header("Authorization", "Bearer " + t)
                .contentType("application/json")
                .content("{\"title\":\"UrgentSoon\",\"priority\":\"URGENT\",\"deadline\":\"2026-08-01T00:00:00Z\"}"));
        mvc.perform(post("/columns/" + done + "/cards").header("Authorization", "Bearer " + t)
                .contentType("application/json")
                .content("{\"title\":\"UrgentLate\",\"priority\":\"URGENT\",\"deadline\":\"2027-01-01T00:00:00Z\"}"));
        mvc.perform(post("/columns/" + todo + "/cards").header("Authorization", "Bearer " + t)
                .contentType("application/json")
                .content("{\"title\":\"LowPrio\",\"priority\":\"LOW\"}"));

        mvc.perform(get("/projects/" + pid + "/cards?priority=URGENT&dueBefore=2026-12-31T00:00:00Z")
                .header("Authorization", "Bearer " + t))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.length()").value(1))
           .andExpect(jsonPath("$[0].title").value("UrgentSoon"));

        mvc.perform(get("/projects/" + pid + "/cards?priority=URGENT")
                .header("Authorization", "Bearer " + t))
           .andExpect(jsonPath("$.length()").value(2));

        mvc.perform(get("/projects/" + pid + "/cards").header("Authorization", "Bearer " + t))
           .andExpect(jsonPath("$.length()").value(3));
    }

    @Test
    void nonMemberGets404OnFilter() throws Exception {
        String t1 = TestUsers.registerAndLogin(mvc, om, "filter2a@test.com");
        String t2 = TestUsers.registerAndLogin(mvc, om, "filter2b@test.com");
        String pid = TestBoards.createProject(mvc, om, t1, "Board");
        mvc.perform(get("/projects/" + pid + "/cards").header("Authorization", "Bearer " + t2))
           .andExpect(status().isNotFound());
    }
}
