package com.gabryel.taskboard.column;

import com.gabryel.taskboard.column.ColumnDtos.*;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
public class ColumnController {

    private final ColumnService service;

    public ColumnController(ColumnService service) {
        this.service = service;
    }

    @GetMapping("/projects/{projectId}/columns")
    public List<ColumnResponse> list(@AuthenticationPrincipal UUID userId,
                                     @PathVariable UUID projectId) {
        return service.list(userId, projectId);
    }

    @PostMapping("/projects/{projectId}/columns")
    @ResponseStatus(HttpStatus.CREATED)
    public ColumnResponse create(@AuthenticationPrincipal UUID userId,
                                 @PathVariable UUID projectId,
                                 @Valid @RequestBody ColumnRequest req) {
        return service.create(userId, projectId, req);
    }

    @PatchMapping("/columns/{id}")
    public ColumnResponse rename(@AuthenticationPrincipal UUID userId, @PathVariable UUID id,
                                 @Valid @RequestBody ColumnRequest req) {
        return service.rename(userId, id, req);
    }

    @PatchMapping("/columns/{id}/position")
    public ColumnResponse move(@AuthenticationPrincipal UUID userId, @PathVariable UUID id,
                               @Valid @RequestBody PositionRequest req) {
        return service.move(userId, id, req.position());
    }

    @DeleteMapping("/columns/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal UUID userId, @PathVariable UUID id) {
        service.delete(userId, id);
    }
}
