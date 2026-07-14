package com.gabryel.taskboard.card;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.UUID;

public class CardDtos {
    public record CardCreateRequest(@NotBlank String title, String description,
                                    Priority priority, Instant deadline, UUID assigneeId) {}
    /** Null field = leave unchanged. clearAssignee/clearDeadline=true explicitly null the field. */
    public record CardPatchRequest(String title, String description, Priority priority,
                                   Instant deadline, UUID assigneeId,
                                   Boolean clearAssignee, Boolean clearDeadline) {}
    public record MoveRequest(@NotNull UUID columnId, @Min(0) int position) {}
    public record CardResponse(UUID id, UUID columnId, String title, String description,
                               Priority priority, Instant deadline, int position,
                               UUID assigneeId, UUID createdById,
                               Instant createdAt, Instant updatedAt) {}

    static CardResponse toResponse(Card c) {
        return new CardResponse(c.getId(), c.getColumn().getId(), c.getTitle(), c.getDescription(),
                c.getPriority(), c.getDeadline(), c.getPosition(),
                c.getAssignee() == null ? null : c.getAssignee().getId(),
                c.getCreatedBy().getId(), c.getCreatedAt(), c.getUpdatedAt());
    }
}
