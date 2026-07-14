package com.gabryel.taskboard.common;

import com.gabryel.taskboard.IntegrationTest;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.web.bind.annotation.*;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Import(ApiExceptionHandlerTest.ThrowingController.class)
class ApiExceptionHandlerTest extends IntegrationTest {

    @TestConfiguration
    @RestController
    static class ThrowingController {
        record Body(@NotBlank String name) {}

        @GetMapping("/test-errors/not-found")
        void notFound() { throw new NotFoundException("thing not found"); }

        @GetMapping("/test-errors/forbidden")
        void forbidden() { throw new ForbiddenException("owner only"); }

        @GetMapping("/test-errors/conflict")
        void conflict() { throw new ConflictException("duplicate"); }

        @GetMapping("/test-errors/unauthorized")
        void unauthorized() { throw new UnauthorizedException("bad token"); }

        @PostMapping("/test-errors/validate")
        void validate(@RequestBody @jakarta.validation.Valid Body body) {}
    }

    @Test
    void notFoundProducesProblemJson() throws Exception {
        mvc.perform(get("/test-errors/not-found").with(user("u")))
           .andExpect(status().isNotFound())
           .andExpect(content().contentType("application/problem+json"))
           .andExpect(jsonPath("$.detail").value("thing not found"));
    }

    @Test
    void forbiddenProduces403() throws Exception {
        mvc.perform(get("/test-errors/forbidden").with(user("u")))
           .andExpect(status().isForbidden())
           .andExpect(content().contentType("application/problem+json"));
    }

    @Test
    void unauthorizedProduces401() throws Exception {
        mvc.perform(get("/test-errors/unauthorized").with(user("u")))
           .andExpect(status().isUnauthorized())
           .andExpect(content().contentType("application/problem+json"));
    }

    @Test
    void conflictProduces409() throws Exception {
        mvc.perform(get("/test-errors/conflict").with(user("u")))
           .andExpect(status().isConflict());
    }

    @Test
    void validationFailureProduces400WithFieldErrors() throws Exception {
        mvc.perform(post("/test-errors/validate").with(user("u"))
                .contentType("application/json").content("{\"name\":\"\"}"))
           .andExpect(status().isBadRequest())
           .andExpect(jsonPath("$.errors.name").exists());
    }
}
