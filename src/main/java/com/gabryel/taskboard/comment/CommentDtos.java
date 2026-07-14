package com.gabryel.taskboard.comment;

import jakarta.validation.constraints.NotBlank;
import java.time.Instant;
import java.util.UUID;

public class CommentDtos {
    public record CommentRequest(@NotBlank String body) {}
    public record CommentResponse(UUID id, UUID authorId, String authorName, String body,
                                  Instant createdAt, Instant updatedAt) {}

    static CommentResponse toResponse(Comment c) {
        return new CommentResponse(c.getId(), c.getAuthor().getId(), c.getAuthor().getName(),
                c.getBody(), c.getCreatedAt(), c.getUpdatedAt());
    }
}
