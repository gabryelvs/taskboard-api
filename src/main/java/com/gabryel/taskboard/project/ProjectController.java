package com.gabryel.taskboard.project;

import com.gabryel.taskboard.project.ProjectDtos.*;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/projects")
public class ProjectController {

    private final ProjectService service;

    public ProjectController(ProjectService service) {
        this.service = service;
    }

    @GetMapping
    public List<ProjectResponse> list(@AuthenticationPrincipal UUID userId) {
        return service.list(userId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProjectResponse create(@AuthenticationPrincipal UUID userId,
                                  @Valid @RequestBody ProjectRequest req) {
        return service.create(userId, req);
    }

    @GetMapping("/{id}")
    public ProjectResponse get(@AuthenticationPrincipal UUID userId, @PathVariable UUID id) {
        return service.get(userId, id);
    }

    @PatchMapping("/{id}")
    public ProjectResponse update(@AuthenticationPrincipal UUID userId, @PathVariable UUID id,
                                  @Valid @RequestBody ProjectRequest req) {
        return service.update(userId, id, req);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal UUID userId, @PathVariable UUID id) {
        service.delete(userId, id);
    }
}
