package com.gabryel.taskboard.comment;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface CommentRepository extends JpaRepository<Comment, UUID> {
    List<Comment> findByCardIdOrderByCreatedAt(UUID cardId);
}
