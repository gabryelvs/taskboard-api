package com.gabryel.taskboard.column;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.UUID;

public interface BoardColumnRepository extends JpaRepository<BoardColumn, UUID> {
    List<BoardColumn> findByProjectIdOrderByPosition(UUID projectId);
    int countByProjectId(UUID projectId);

    @Modifying
    @Query("update BoardColumn b set b.position = b.position - 1 where b.project.id = :projectId and b.position > :position")
    void shiftLeftAfter(UUID projectId, int position);

    @Modifying
    @Query("update BoardColumn b set b.position = b.position + 1 where b.project.id = :projectId and b.position >= :position")
    void shiftRightFrom(UUID projectId, int position);
}
