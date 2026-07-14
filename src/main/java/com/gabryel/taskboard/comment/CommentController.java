package com.gabryel.taskboard.comment;

import com.gabryel.taskboard.comment.CommentDtos.*;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
public class CommentController {

    private final CommentService service;

    public CommentController(CommentService service) {
        this.service = service;
    }

    @GetMapping("/cards/{cardId}/comments")
    public List<CommentResponse> list(@AuthenticationPrincipal UUID userId,
                                      @PathVariable UUID cardId) {
        return service.list(userId, cardId);
    }

    @PostMapping("/cards/{cardId}/comments")
    @ResponseStatus(HttpStatus.CREATED)
    public CommentResponse create(@AuthenticationPrincipal UUID userId,
                                  @PathVariable UUID cardId,
                                  @Valid @RequestBody CommentRequest req) {
        return service.create(userId, cardId, req);
    }

    @PatchMapping("/comments/{id}")
    public CommentResponse update(@AuthenticationPrincipal UUID userId, @PathVariable UUID id,
                                  @Valid @RequestBody CommentRequest req) {
        return service.update(userId, id, req);
    }

    @DeleteMapping("/comments/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal UUID userId, @PathVariable UUID id) {
        service.delete(userId, id);
    }
}
