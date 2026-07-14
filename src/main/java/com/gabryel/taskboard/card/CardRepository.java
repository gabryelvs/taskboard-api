package com.gabryel.taskboard.card;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface CardRepository extends JpaRepository<Card, UUID> {
    List<Card> findByColumnIdOrderByPosition(UUID columnId);
    int countByColumnId(UUID columnId);
}
