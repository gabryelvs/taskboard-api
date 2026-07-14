package com.gabryel.taskboard.card;

import com.gabryel.taskboard.IntegrationTest;
import com.gabryel.taskboard.TestBoards;
import com.gabryel.taskboard.TestUsers;
import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class CardMoveTest extends IntegrationTest {

    private void move(String token, String cardId, String columnId, int position, int expectStatus) throws Exception {
        mvc.perform(patch("/cards/" + cardId + "/move")
                .header("Authorization", "Bearer " + token)
                .contentType("application/json")
                .content("{\"columnId\":\"" + columnId + "\",\"position\":" + position + "}"))
           .andExpect(status().is(expectStatus));
    }

    @Test
    void moveAcrossColumnsKeepsBothColumnsDense() throws Exception {
        String t = TestUsers.registerAndLogin(mvc, om, "move1@test.com");
        String pid = TestBoards.createProject(mvc, om, t, "Board");
        String todo = TestBoards.createColumn(mvc, om, t, pid, "Todo");
        String done = TestBoards.createColumn(mvc, om, t, pid, "Done");
        TestBoards.createCard(mvc, om, t, todo, "A");
        String b = TestBoards.createCard(mvc, om, t, todo, "B");
        TestBoards.createCard(mvc, om, t, todo, "C");
        TestBoards.createCard(mvc, om, t, done, "X");

        move(t, b, done, 0, 200);

        // source: A(0), C(1)
        mvc.perform(get("/columns/" + todo + "/cards").header("Authorization", "Bearer " + t))
           .andExpect(jsonPath("$.length()").value(2))
           .andExpect(jsonPath("$[0].title").value("A"))
           .andExpect(jsonPath("$[1].title").value("C"))
           .andExpect(jsonPath("$[1].position").value(1));
        // target: B(0), X(1)
        mvc.perform(get("/columns/" + done + "/cards").header("Authorization", "Bearer " + t))
           .andExpect(jsonPath("$[0].title").value("B"))
           .andExpect(jsonPath("$[1].title").value("X"));
    }

    @Test
    void moveWithinSameColumn() throws Exception {
        String t = TestUsers.registerAndLogin(mvc, om, "move2@test.com");
        String pid = TestBoards.createProject(mvc, om, t, "Board");
        String col = TestBoards.createColumn(mvc, om, t, pid, "Todo");
        String a = TestBoards.createCard(mvc, om, t, col, "A");
        TestBoards.createCard(mvc, om, t, col, "B");
        TestBoards.createCard(mvc, om, t, col, "C");

        move(t, a, col, 2, 200);

        mvc.perform(get("/columns/" + col + "/cards").header("Authorization", "Bearer " + t))
           .andExpect(jsonPath("$[0].title").value("B"))
           .andExpect(jsonPath("$[1].title").value("C"))
           .andExpect(jsonPath("$[2].title").value("A"));
    }

    @Test
    void positionBeyondEndIsClamped() throws Exception {
        String t = TestUsers.registerAndLogin(mvc, om, "move3@test.com");
        String pid = TestBoards.createProject(mvc, om, t, "Board");
        String col = TestBoards.createColumn(mvc, om, t, pid, "Todo");
        String done = TestBoards.createColumn(mvc, om, t, pid, "Done");
        String a = TestBoards.createCard(mvc, om, t, col, "A");

        move(t, a, done, 99, 200);

        mvc.perform(get("/columns/" + done + "/cards").header("Authorization", "Bearer " + t))
           .andExpect(jsonPath("$[0].title").value("A"))
           .andExpect(jsonPath("$[0].position").value(0));
    }

    @Test
    void moveToColumnInAnotherProjectIs404() throws Exception {
        String t = TestUsers.registerAndLogin(mvc, om, "move4@test.com");
        String pid1 = TestBoards.createProject(mvc, om, t, "Board1");
        String pid2 = TestBoards.createProject(mvc, om, t, "Board2");
        String col1 = TestBoards.createColumn(mvc, om, t, pid1, "Todo");
        String col2 = TestBoards.createColumn(mvc, om, t, pid2, "Todo");
        String a = TestBoards.createCard(mvc, om, t, col1, "A");

        move(t, a, col2, 0, 404);
    }

    @Test
    void concurrentMovesKeepPositionsDense() throws Exception {
        String t = TestUsers.registerAndLogin(mvc, om, "move5@test.com");
        String pid = TestBoards.createProject(mvc, om, t, "Board");
        String col = TestBoards.createColumn(mvc, om, t, pid, "Todo");
        String done = TestBoards.createColumn(mvc, om, t, pid, "Done");
        java.util.List<String> ids = new java.util.ArrayList<>();
        for (int i = 0; i < 5; i++) ids.add(TestBoards.createCard(mvc, om, t, col, "N" + i));

        // fire 5 moves into the same target column from parallel threads
        var pool = java.util.concurrent.Executors.newFixedThreadPool(5);
        var futures = ids.stream().map(id -> pool.submit(() -> {
            mvc.perform(patch("/cards/" + id + "/move")
                    .header("Authorization", "Bearer " + t)
                    .contentType("application/json")
                    .content("{\"columnId\":\"" + done + "\",\"position\":0}"))
               .andReturn();
            return null;
        })).toList();
        for (var f : futures) f.get();
        pool.shutdown();

        // invariant: target column holds all 5 cards with dense positions 0..4
        String body = mvc.perform(get("/columns/" + done + "/cards")
                .header("Authorization", "Bearer " + t))
                .andReturn().getResponse().getContentAsString();
        var arr = om.readTree(body);
        org.assertj.core.api.Assertions.assertThat(arr.size()).isEqualTo(5);
        for (int i = 0; i < 5; i++) {
            org.assertj.core.api.Assertions.assertThat(arr.get(i).get("position").asInt()).isEqualTo(i);
        }
    }
}
