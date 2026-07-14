package com.gabryel.taskboard.card;

import com.gabryel.taskboard.auth.User;
import com.gabryel.taskboard.auth.UserRepository;
import com.gabryel.taskboard.card.CardDtos.*;
import com.gabryel.taskboard.column.BoardColumn;
import com.gabryel.taskboard.column.ColumnService;
import com.gabryel.taskboard.common.NotFoundException;
import com.gabryel.taskboard.common.ProjectAccessService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class CardService {

    private final CardRepository cards;
    private final UserRepository users;
    private final ColumnService columnService;
    private final ProjectAccessService access;

    public CardService(CardRepository cards, UserRepository users,
                       ColumnService columnService, ProjectAccessService access) {
        this.cards = cards;
        this.users = users;
        this.columnService = columnService;
        this.access = access;
    }

    /** Loads a card and verifies the caller is a member of its project (404 otherwise). */
    @Transactional(readOnly = true)
    public Card requireCard(UUID cardId, UUID userId) {
        Card card = cards.findById(cardId)
                .orElseThrow(() -> new NotFoundException("Card not found"));
        access.requireMember(card.getColumn().getProject().getId(), userId);
        return card;
    }

    @Transactional(readOnly = true)
    public List<CardResponse> listByColumn(UUID userId, UUID columnId) {
        columnService.requireColumn(columnId, userId);
        return cards.findByColumnIdOrderByPosition(columnId).stream()
                .map(CardDtos::toResponse).toList();
    }

    @Transactional
    public CardResponse create(UUID userId, UUID columnId, CardCreateRequest req) {
        BoardColumn col = columnService.requireColumn(columnId, userId);
        Card card = new Card();
        card.setColumn(col);
        card.setTitle(req.title());
        card.setDescription(req.description());
        if (req.priority() != null) card.setPriority(req.priority());
        card.setDeadline(req.deadline());
        if (req.assigneeId() != null) {
            card.setAssignee(resolveMember(col.getProject().getId(), req.assigneeId()));
        }
        card.setCreatedBy(users.getReferenceById(userId));
        card.setPosition(cards.countByColumnId(columnId));
        return CardDtos.toResponse(cards.save(card));
    }

    @Transactional(readOnly = true)
    public CardResponse get(UUID userId, UUID cardId) {
        return CardDtos.toResponse(requireCard(cardId, userId));
    }

    @Transactional
    public CardResponse patch(UUID userId, UUID cardId, CardPatchRequest req) {
        Card card = requireCard(cardId, userId);
        if (req.title() != null) card.setTitle(req.title());
        if (req.description() != null) card.setDescription(req.description());
        if (req.priority() != null) card.setPriority(req.priority());
        if (req.deadline() != null) card.setDeadline(req.deadline());
        if (Boolean.TRUE.equals(req.clearDeadline())) card.setDeadline(null);
        if (req.assigneeId() != null) {
            card.setAssignee(resolveMember(card.getColumn().getProject().getId(), req.assigneeId()));
        }
        if (Boolean.TRUE.equals(req.clearAssignee())) card.setAssignee(null);
        return CardDtos.toResponse(cards.save(card));
    }

    @Transactional
    public void delete(UUID userId, UUID cardId) {
        Card card = requireCard(cardId, userId);
        UUID columnId = card.getColumn().getId();
        int pos = card.getPosition();
        cards.delete(card);
        cards.flush();
        cards.shiftDownAfter(columnId, pos);
    }

    private User resolveMember(UUID projectId, UUID assigneeId) {
        try {
            access.requireMember(projectId, assigneeId);
        } catch (NotFoundException e) {
            throw new NotFoundException("Assignee is not a project member");
        }
        return users.getReferenceById(assigneeId);
    }
}
