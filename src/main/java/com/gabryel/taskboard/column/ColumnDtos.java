package com.gabryel.taskboard.column;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.util.UUID;

public class ColumnDtos {
    public record ColumnRequest(@NotBlank String name) {}
    public record PositionRequest(@Min(0) int position) {}
    public record ColumnResponse(UUID id, String name, int position) {}

    static ColumnResponse toResponse(BoardColumn c) {
        return new ColumnResponse(c.getId(), c.getName(), c.getPosition());
    }
}
