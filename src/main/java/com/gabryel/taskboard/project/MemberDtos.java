package com.gabryel.taskboard.project;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import java.util.UUID;

public class MemberDtos {
    public record MemberRequest(@Email @NotBlank String email) {}
    public record MemberResponse(UUID userId, String email, String name, String role) {}

    static MemberResponse toResponse(ProjectMember pm) {
        return new MemberResponse(pm.getUser().getId(), pm.getUser().getEmail(),
                pm.getUser().getName(), pm.getRole().name());
    }
}
