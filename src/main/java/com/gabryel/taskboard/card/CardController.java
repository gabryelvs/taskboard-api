package com.gabryel.taskboard.card;

import com.gabryel.taskboard.card.CardDtos.*;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
public class CardController {

    private final CardService service;

    public CardController(CardService service) {
        this.service = service;
    }

    @GetMapping("/columns/{columnId}/cards")
    public List<CardResponse> listByColumn(@AuthenticationPrincipal UUID userId,
                                           @PathVariable UUID columnId) {
        return service.listByColumn(userId, columnId);
    }

    @PostMapping("/columns/{columnId}/cards")
    @ResponseStatus(HttpStatus.CREATED)
    public CardResponse create(@AuthenticationPrincipal UUID userId,
                               @PathVariable UUID columnId,
                               @Valid @RequestBody CardCreateRequest req) {
        return service.create(userId, columnId, req);
    }

    @GetMapping("/cards/{id}")
    public CardResponse get(@AuthenticationPrincipal UUID userId, @PathVariable UUID id) {
        return service.get(userId, id);
    }

    @PatchMapping("/cards/{id}")
    public CardResponse patch(@AuthenticationPrincipal UUID userId, @PathVariable UUID id,
                              @Valid @RequestBody CardPatchRequest req) {
        return service.patch(userId, id, req);
    }

    @DeleteMapping("/cards/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal UUID userId, @PathVariable UUID id) {
        service.delete(userId, id);
    }

    @PatchMapping("/cards/{id}/move")
    public CardResponse move(@AuthenticationPrincipal UUID userId, @PathVariable UUID id,
                             @Valid @RequestBody MoveRequest req) {
        return service.move(userId, id, req);
    }
}
