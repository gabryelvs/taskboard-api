package com.gabryel.taskboard.column;

import com.gabryel.taskboard.IntegrationTest;
import com.gabryel.taskboard.TestBoards;
import com.gabryel.taskboard.TestUsers;
import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ColumnTest extends IntegrationTest {

    @Test
    void columnsAppendInOrder() throws Exception {
        String t = TestUsers.registerAndLogin(mvc, om, "col1@test.com");
        String pid = TestBoards.createProject(mvc, om, t, "Board");
        TestBoards.createColumn(mvc, om, t, pid, "Todo");
        TestBoards.createColumn(mvc, om, t, pid, "Doing");
        TestBoards.createColumn(mvc, om, t, pid, "Done");

        mvc.perform(get("/projects/" + pid + "/columns").header("Authorization", "Bearer " + t))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$[0].name").value("Todo"))
           .andExpect(jsonPath("$[0].position").value(0))
           .andExpect(jsonPath("$[2].name").value("Done"))
           .andExpect(jsonPath("$[2].position").value(2));
    }

    @Test
    void moveColumnShiftsSiblings() throws Exception {
        String t = TestUsers.registerAndLogin(mvc, om, "col2@test.com");
        String pid = TestBoards.createProject(mvc, om, t, "Board");
        TestBoards.createColumn(mvc, om, t, pid, "A");
        TestBoards.createColumn(mvc, om, t, pid, "B");
        String cId = TestBoards.createColumn(mvc, om, t, pid, "C");

        mvc.perform(patch("/columns/" + cId + "/position")
                .header("Authorization", "Bearer " + t)
                .contentType("application/json").content("{\"position\":0}"))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.position").value(0));

        mvc.perform(get("/projects/" + pid + "/columns").header("Authorization", "Bearer " + t))
           .andExpect(jsonPath("$[0].name").value("C"))
           .andExpect(jsonPath("$[1].name").value("A"))
           .andExpect(jsonPath("$[2].name").value("B"));
    }

    @Test
    void deleteColumnClosesGap() throws Exception {
        String t = TestUsers.registerAndLogin(mvc, om, "col3@test.com");
        String pid = TestBoards.createProject(mvc, om, t, "Board");
        TestBoards.createColumn(mvc, om, t, pid, "A");
        String bId = TestBoards.createColumn(mvc, om, t, pid, "B");
        TestBoards.createColumn(mvc, om, t, pid, "C");

        mvc.perform(delete("/columns/" + bId).header("Authorization", "Bearer " + t))
           .andExpect(status().isNoContent());

        mvc.perform(get("/projects/" + pid + "/columns").header("Authorization", "Bearer " + t))
           .andExpect(jsonPath("$.length()").value(2))
           .andExpect(jsonPath("$[1].name").value("C"))
           .andExpect(jsonPath("$[1].position").value(1));
    }

    @Test
    void renameColumn() throws Exception {
        String t = TestUsers.registerAndLogin(mvc, om, "col4@test.com");
        String pid = TestBoards.createProject(mvc, om, t, "Board");
        String id = TestBoards.createColumn(mvc, om, t, pid, "Old");
        mvc.perform(patch("/columns/" + id).header("Authorization", "Bearer " + t)
                .contentType("application/json").content("{\"name\":\"New\"}"))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.name").value("New"));
    }

    @Test
    void nonMemberGets404OnColumn() throws Exception {
        String t1 = TestUsers.registerAndLogin(mvc, om, "col5a@test.com");
        String t2 = TestUsers.registerAndLogin(mvc, om, "col5b@test.com");
        String pid = TestBoards.createProject(mvc, om, t1, "Board");
        String id = TestBoards.createColumn(mvc, om, t1, pid, "Hidden");
        mvc.perform(patch("/columns/" + id).header("Authorization", "Bearer " + t2)
                .contentType("application/json").content("{\"name\":\"Hack\"}"))
           .andExpect(status().isNotFound());
    }
}
