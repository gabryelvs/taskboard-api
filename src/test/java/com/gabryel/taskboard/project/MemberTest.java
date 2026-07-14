package com.gabryel.taskboard.project;

import com.gabryel.taskboard.IntegrationTest;
import com.gabryel.taskboard.TestBoards;
import com.gabryel.taskboard.TestUsers;
import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class MemberTest extends IntegrationTest {

    @Test
    void ownerInvitesMemberWhoCanThenSeeProject() throws Exception {
        String owner = TestUsers.registerAndLogin(mvc, om, "mem1a@test.com");
        String invitee = TestUsers.registerAndLogin(mvc, om, "mem1b@test.com");
        String pid = TestBoards.createProject(mvc, om, owner, "Shared");

        mvc.perform(post("/projects/" + pid + "/members")
                .header("Authorization", "Bearer " + owner)
                .contentType("application/json").content("{\"email\":\"mem1b@test.com\"}"))
           .andExpect(status().isCreated())
           .andExpect(jsonPath("$.role").value("MEMBER"));

        mvc.perform(get("/projects/" + pid).header("Authorization", "Bearer " + invitee))
           .andExpect(status().isOk());
    }

    @Test
    void memberCannotInvite() throws Exception {
        String owner = TestUsers.registerAndLogin(mvc, om, "mem2a@test.com");
        String member = TestUsers.registerAndLogin(mvc, om, "mem2b@test.com");
        TestUsers.registerAndLogin(mvc, om, "mem2c@test.com");
        String pid = TestBoards.createProject(mvc, om, owner, "Board");
        mvc.perform(post("/projects/" + pid + "/members")
                .header("Authorization", "Bearer " + owner)
                .contentType("application/json").content("{\"email\":\"mem2b@test.com\"}"));

        mvc.perform(post("/projects/" + pid + "/members")
                .header("Authorization", "Bearer " + member)
                .contentType("application/json").content("{\"email\":\"mem2c@test.com\"}"))
           .andExpect(status().isForbidden());
    }

    @Test
    void duplicateInviteIs409() throws Exception {
        String owner = TestUsers.registerAndLogin(mvc, om, "mem3a@test.com");
        TestUsers.registerAndLogin(mvc, om, "mem3b@test.com");
        String pid = TestBoards.createProject(mvc, om, owner, "Board");
        String invite = "{\"email\":\"mem3b@test.com\"}";
        mvc.perform(post("/projects/" + pid + "/members").header("Authorization", "Bearer " + owner)
                .contentType("application/json").content(invite)).andExpect(status().isCreated());
        mvc.perform(post("/projects/" + pid + "/members").header("Authorization", "Bearer " + owner)
                .contentType("application/json").content(invite)).andExpect(status().isConflict());
    }

    @Test
    void unknownEmailIs404() throws Exception {
        String owner = TestUsers.registerAndLogin(mvc, om, "mem4@test.com");
        String pid = TestBoards.createProject(mvc, om, owner, "Board");
        mvc.perform(post("/projects/" + pid + "/members").header("Authorization", "Bearer " + owner)
                .contentType("application/json").content("{\"email\":\"ghost@test.com\"}"))
           .andExpect(status().isNotFound());
    }

    @Test
    void ownerRemovesMemberWhoLosesAccess() throws Exception {
        String owner = TestUsers.registerAndLogin(mvc, om, "mem5a@test.com");
        String member = TestUsers.registerAndLogin(mvc, om, "mem5b@test.com");
        String pid = TestBoards.createProject(mvc, om, owner, "Board");
        String memberId = om.readTree(mvc.perform(post("/projects/" + pid + "/members")
                .header("Authorization", "Bearer " + owner)
                .contentType("application/json").content("{\"email\":\"mem5b@test.com\"}"))
                .andReturn().getResponse().getContentAsString()).get("userId").asText();

        mvc.perform(delete("/projects/" + pid + "/members/" + memberId)
                .header("Authorization", "Bearer " + owner))
           .andExpect(status().isNoContent());
        mvc.perform(get("/projects/" + pid).header("Authorization", "Bearer " + member))
           .andExpect(status().isNotFound());
    }

    @Test
    void ownerCannotRemoveSelf() throws Exception {
        String owner = TestUsers.registerAndLogin(mvc, om, "mem6@test.com");
        String pid = TestBoards.createProject(mvc, om, owner, "Board");
        String ownerId = om.readTree(mvc.perform(get("/projects/" + pid)
                .header("Authorization", "Bearer " + owner))
                .andReturn().getResponse().getContentAsString()).get("ownerId").asText();
        mvc.perform(delete("/projects/" + pid + "/members/" + ownerId)
                .header("Authorization", "Bearer " + owner))
           .andExpect(status().isConflict());
    }
}
