package com.gabryel.taskboard.comment;

import com.gabryel.taskboard.auth.UserRepository;
import com.gabryel.taskboard.card.Card;
import com.gabryel.taskboard.card.CardService;
import com.gabryel.taskboard.comment.CommentDtos.*;
import com.gabryel.taskboard.common.ForbiddenException;
import com.gabryel.taskboard.common.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class CommentService {

    private final CommentRepository comments;
    private final UserRepository users;
    private final CardService cardService;

    public CommentService(CommentRepository comments, UserRepository users, CardService cardService) {
        this.comments = comments;
        this.users = users;
        this.cardService = cardService;
    }

    @Transactional(readOnly = true)
    public List<CommentResponse> list(UUID userId, UUID cardId) {
        cardService.requireCard(cardId, userId);
        return comments.findByCardIdOrderByCreatedAt(cardId).stream()
                .map(CommentDtos::toResponse).toList();
    }

    @Transactional
    public CommentResponse create(UUID userId, UUID cardId, CommentRequest req) {
        Card card = cardService.requireCard(cardId, userId);
        Comment c = new Comment();
        c.setCard(card);
        c.setAuthor(users.getReferenceById(userId));
        c.setBody(req.body());
        return CommentDtos.toResponse(comments.save(c));
    }

    @Transactional
    public CommentResponse update(UUID userId, UUID commentId, CommentRequest req) {
        Comment c = requireAuthoredComment(commentId, userId);
        c.setBody(req.body());
        return CommentDtos.toResponse(comments.save(c));
    }

    @Transactional
    public void delete(UUID userId, UUID commentId) {
        comments.delete(requireAuthoredComment(commentId, userId));
    }

    private Comment requireAuthoredComment(UUID commentId, UUID userId) {
        Comment c = comments.findById(commentId)
                .orElseThrow(() -> new NotFoundException("Comment not found"));
        // membership check first: non-members must see 404, not 403
        cardService.requireCard(c.getCard().getId(), userId);
        if (!c.getAuthor().getId().equals(userId)) {
            throw new ForbiddenException("Only the author can modify this comment");
        }
        return c;
    }
}
