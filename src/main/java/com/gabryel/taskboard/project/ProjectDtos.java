package com.gabryel.taskboard.project;

import jakarta.validation.constraints.NotBlank;
import java.time.Instant;
import java.util.UUID;

public class ProjectDtos {
    public record ProjectRequest(@NotBlank String name, String description) {}
    public record ProjectResponse(UUID id, String name, String description,
                                  UUID ownerId, Instant createdAt) {}

    static ProjectResponse toResponse(Project p) {
        return new ProjectResponse(p.getId(), p.getName(), p.getDescription(),
                p.getOwner().getId(), p.getCreatedAt());
    }
}
