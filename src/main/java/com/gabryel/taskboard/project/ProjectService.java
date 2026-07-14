package com.gabryel.taskboard.project;

import com.gabryel.taskboard.auth.User;
import com.gabryel.taskboard.auth.UserRepository;
import com.gabryel.taskboard.common.NotFoundException;
import com.gabryel.taskboard.common.ProjectAccessService;
import com.gabryel.taskboard.project.ProjectDtos.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class ProjectService {

    private final ProjectRepository projects;
    private final ProjectMemberRepository members;
    private final UserRepository users;
    private final ProjectAccessService access;

    public ProjectService(ProjectRepository projects, ProjectMemberRepository members,
                          UserRepository users, ProjectAccessService access) {
        this.projects = projects;
        this.members = members;
        this.users = users;
        this.access = access;
    }

    @Transactional
    public ProjectResponse create(UUID userId, ProjectRequest req) {
        User owner = users.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));
        Project p = new Project();
        p.setName(req.name());
        p.setDescription(req.description());
        p.setOwner(owner);
        projects.save(p);

        ProjectMember pm = new ProjectMember();
        pm.setProject(p);
        pm.setUser(owner);
        pm.setRole(Role.OWNER);
        members.save(pm);

        return ProjectDtos.toResponse(p);
    }

    @Transactional(readOnly = true)
    public List<ProjectResponse> list(UUID userId) {
        return members.findByUserId(userId).stream()
                .map(pm -> ProjectDtos.toResponse(pm.getProject()))
                .toList();
    }

    @Transactional(readOnly = true)
    public ProjectResponse get(UUID userId, UUID projectId) {
        access.requireMember(projectId, userId);
        return ProjectDtos.toResponse(projects.findById(projectId)
                .orElseThrow(() -> new NotFoundException("Project not found")));
    }

    @Transactional
    public ProjectResponse update(UUID userId, UUID projectId, ProjectRequest req) {
        access.requireOwner(projectId, userId);
        Project p = projects.findById(projectId)
                .orElseThrow(() -> new NotFoundException("Project not found"));
        p.setName(req.name());
        p.setDescription(req.description());
        return ProjectDtos.toResponse(projects.save(p));
    }

    @Transactional
    public void delete(UUID userId, UUID projectId) {
        access.requireOwner(projectId, userId);
        projects.deleteById(projectId); // DB cascades: members, columns -> cards -> comments
    }
}
