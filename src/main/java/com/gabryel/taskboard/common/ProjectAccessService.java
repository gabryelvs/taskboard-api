package com.gabryel.taskboard.common;

import com.gabryel.taskboard.project.ProjectMember;
import com.gabryel.taskboard.project.ProjectMemberRepository;
import com.gabryel.taskboard.project.Role;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class ProjectAccessService {

    private final ProjectMemberRepository members;

    public ProjectAccessService(ProjectMemberRepository members) {
        this.members = members;
    }

    /** 404 whether the project is missing or the caller simply isn't a member — no existence leak. */
    public ProjectMember requireMember(UUID projectId, UUID userId) {
        return members.findByProjectIdAndUserId(projectId, userId)
                .orElseThrow(() -> new NotFoundException("Project not found"));
    }

    public ProjectMember requireOwner(UUID projectId, UUID userId) {
        ProjectMember member = requireMember(projectId, userId);
        if (member.getRole() != Role.OWNER) {
            throw new ForbiddenException("Owner role required");
        }
        return member;
    }
}
