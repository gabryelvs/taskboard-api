package com.gabryel.taskboard.column;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface BoardColumnRepository extends JpaRepository<BoardColumn, UUID> {
    List<BoardColumn> findByProjectIdOrderByPosition(UUID projectId);
    int countByProjectId(UUID projectId);
}
