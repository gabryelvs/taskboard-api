package com.gabryel.taskboard.column;

import com.gabryel.taskboard.column.ColumnDtos.*;
import com.gabryel.taskboard.common.NotFoundException;
import com.gabryel.taskboard.common.ProjectAccessService;
import com.gabryel.taskboard.project.ProjectRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class ColumnService {

    private final BoardColumnRepository columns;
    private final ProjectRepository projects;
    private final ProjectAccessService access;

    public ColumnService(BoardColumnRepository columns, ProjectRepository projects,
                         ProjectAccessService access) {
        this.columns = columns;
        this.projects = projects;
        this.access = access;
    }

    /** Loads a column and verifies the caller is a member of its project (404 otherwise). */
    @Transactional(readOnly = true)
    public BoardColumn requireColumn(UUID columnId, UUID userId) {
        BoardColumn col = columns.findById(columnId)
                .orElseThrow(() -> new NotFoundException("Column not found"));
        access.requireMember(col.getProject().getId(), userId);
        return col;
    }

    @Transactional(readOnly = true)
    public List<ColumnResponse> list(UUID userId, UUID projectId) {
        access.requireMember(projectId, userId);
        return columns.findByProjectIdOrderByPosition(projectId).stream()
                .map(ColumnDtos::toResponse).toList();
    }

    @Transactional
    public ColumnResponse create(UUID userId, UUID projectId, ColumnRequest req) {
        access.requireMember(projectId, userId);
        BoardColumn col = new BoardColumn();
        col.setProject(projects.getReferenceById(projectId));
        col.setName(req.name());
        col.setPosition(columns.countByProjectId(projectId));
        return ColumnDtos.toResponse(columns.save(col));
    }

    @Transactional
    public ColumnResponse rename(UUID userId, UUID columnId, ColumnRequest req) {
        BoardColumn col = requireColumn(columnId, userId);
        col.setName(req.name());
        return ColumnDtos.toResponse(columns.save(col));
    }

    @Transactional
    public ColumnResponse move(UUID userId, UUID columnId, int position) {
        BoardColumn col = requireColumn(columnId, userId);
        UUID projectId = col.getProject().getId();
        int count = columns.countByProjectId(projectId);
        int target = Math.min(Math.max(position, 0), count - 1);

        columns.shiftLeftAfter(projectId, col.getPosition());   // close gap at old slot
        columns.shiftRightFrom(projectId, target);              // open gap at new slot
        col.setPosition(target);
        return ColumnDtos.toResponse(columns.save(col));
    }

    @Transactional
    public void delete(UUID userId, UUID columnId) {
        BoardColumn col = requireColumn(columnId, userId);
        UUID projectId = col.getProject().getId();
        int pos = col.getPosition();
        columns.delete(col);
        columns.flush(); // delete before shifting so unique/dense invariant holds
        columns.shiftLeftAfter(projectId, pos);
    }
}
