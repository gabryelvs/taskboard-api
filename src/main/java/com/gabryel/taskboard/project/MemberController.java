package com.gabryel.taskboard.project;

import com.gabryel.taskboard.project.MemberDtos.*;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/projects/{projectId}/members")
public class MemberController {

    private final MemberService service;

    public MemberController(MemberService service) {
        this.service = service;
    }

    @GetMapping
    public List<MemberResponse> list(@AuthenticationPrincipal UUID userId,
                                     @PathVariable UUID projectId) {
        return service.list(userId, projectId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MemberResponse invite(@AuthenticationPrincipal UUID userId,
                                 @PathVariable UUID projectId,
                                 @Valid @RequestBody MemberRequest req) {
        return service.invite(userId, projectId, req);
    }

    @DeleteMapping("/{targetUserId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remove(@AuthenticationPrincipal UUID userId,
                       @PathVariable UUID projectId,
                       @PathVariable UUID targetUserId) {
        service.remove(userId, projectId, targetUserId);
    }
}
