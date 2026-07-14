package com.gabryel.taskboard.common;

import com.gabryel.taskboard.IntegrationTest;
import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class OpenApiTest extends IntegrationTest {

    @Test
    void apiDocsArePublicAndDescribeTheApi() throws Exception {
        mvc.perform(get("/v3/api-docs"))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.info.title").value("Taskboard API"))
           .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.scheme").value("bearer"))
           .andExpect(jsonPath("$.paths['/auth/register']").exists())
           .andExpect(jsonPath("$.paths['/cards/{id}/move']").exists());
    }
}
