package com.gabryel.taskboard.card;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import java.time.Instant;
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

    @Query("""
        select c from Card c
        where c.column.project.id = :projectId
          and (:priority is null or c.priority = :priority)
          and (cast(:dueBefore as timestamp) is null or c.deadline < :dueBefore)
          and (:assigneeId is null or c.assignee.id = :assigneeId)
        order by c.deadline asc nulls last, c.priority desc
        """)
    List<Card> filter(UUID projectId, Priority priority, Instant dueBefore, UUID assigneeId);
}
