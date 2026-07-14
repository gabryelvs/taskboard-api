package com.gabryel.taskboard.project;

import com.gabryel.taskboard.auth.User;
import com.gabryel.taskboard.auth.UserRepository;
import com.gabryel.taskboard.card.CardRepository;
import com.gabryel.taskboard.common.ConflictException;
import com.gabryel.taskboard.common.NotFoundException;
import com.gabryel.taskboard.common.ProjectAccessService;
import com.gabryel.taskboard.project.MemberDtos.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class MemberService {

    private final ProjectMemberRepository members;
    private final ProjectRepository projects;
    private final UserRepository users;
    private final CardRepository cards;
    private final ProjectAccessService access;

    public MemberService(ProjectMemberRepository members, ProjectRepository projects,
                         UserRepository users, CardRepository cards, ProjectAccessService access) {
        this.members = members;
        this.projects = projects;
        this.users = users;
        this.cards = cards;
        this.access = access;
    }

    @Transactional(readOnly = true)
    public List<MemberResponse> list(UUID userId, UUID projectId) {
        access.requireMember(projectId, userId);
        return members.findByProjectId(projectId).stream()
                .map(MemberDtos::toResponse).toList();
    }

    @Transactional
    public MemberResponse invite(UUID userId, UUID projectId, MemberRequest req) {
        access.requireOwner(projectId, userId);
        User target = users.findByEmail(req.email())
                .orElseThrow(() -> new NotFoundException("User not found"));
        if (members.findByProjectIdAndUserId(projectId, target.getId()).isPresent()) {
            throw new ConflictException("Already a member");
        }
        ProjectMember pm = new ProjectMember();
        pm.setProject(projects.getReferenceById(projectId));
        pm.setUser(target);
        pm.setRole(Role.MEMBER);
        members.save(pm);
        return MemberDtos.toResponse(pm);
    }

    @Transactional
    public void remove(UUID userId, UUID projectId, UUID targetUserId) {
        access.requireOwner(projectId, userId);
        if (userId.equals(targetUserId)) {
            throw new ConflictException("Owner cannot remove themselves");
        }
        ProjectMember target = members.findByProjectIdAndUserId(projectId, targetUserId)
                .orElseThrow(() -> new NotFoundException("Member not found"));
        cards.unassignUserInProject(projectId, targetUserId);
        members.delete(target);
    }
}
