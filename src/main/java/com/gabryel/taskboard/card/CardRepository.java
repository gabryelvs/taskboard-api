package com.gabryel.taskboard.card;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.UUID;

public interface CardRepository extends JpaRepository<Card, UUID> {
    List<Card> findByColumnIdOrderByPosition(UUID columnId);
    int countByColumnId(UUID columnId);

    @Modifying
    @Query("update Card c set c.assignee = null where c.assignee.id = :userId and c.column.project.id = :projectId")
    void unassignUserInProject(UUID projectId, UUID userId);

    @Modifying
    @Query("update Card c set c.position = c.position - 1 where c.column.id = :columnId and c.position > :position")
    void shiftDownAfter(UUID columnId, int position);

    @Modifying
    @Query("update Card c set c.position = c.position + 1 where c.column.id = :columnId and c.position >= :position")
    void shiftUpFrom(UUID columnId, int position);
}
