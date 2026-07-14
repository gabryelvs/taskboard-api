package com.gabryel.taskboard.card;

import com.gabryel.taskboard.auth.User;
import com.gabryel.taskboard.auth.UserRepository;
import com.gabryel.taskboard.card.CardDtos.*;
import com.gabryel.taskboard.column.BoardColumn;
import com.gabryel.taskboard.column.BoardColumnRepository;
import com.gabryel.taskboard.column.ColumnService;
import com.gabryel.taskboard.common.ConflictException;
import com.gabryel.taskboard.common.NotFoundException;
import com.gabryel.taskboard.common.ProjectAccessService;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class CardService {

    private final CardRepository cards;
    private final UserRepository users;
    private final ColumnService columnService;
    private final BoardColumnRepository columns;
    private final ProjectAccessService access;
    private final EntityManager em;

    public CardService(CardRepository cards, UserRepository users,
                       ColumnService columnService, BoardColumnRepository columns,
                       ProjectAccessService access, EntityManager em) {
        this.cards = cards;
        this.users = users;
        this.columnService = columnService;
        this.columns = columns;
        this.access = access;
        this.em = em;
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

    @Transactional
    public CardResponse move(UUID userId, UUID cardId, MoveRequest req) {
        Card card = requireCard(cardId, userId);
        BoardColumn target = columnService.requireColumn(req.columnId(), userId);
        if (!target.getProject().getId().equals(card.getColumn().getProject().getId())) {
            throw new NotFoundException("Column not found");
        }

        // serialize concurrent moves per project: lock source and target column rows
        // in deterministic id order to avoid deadlock
        Set<UUID> lockedColumnIds = java.util.stream.Stream.of(card.getColumn().getId(), target.getId())
                .distinct().sorted()
                .peek(colId -> columns.lockById(colId))
                .collect(java.util.stream.Collectors.toSet());

        // re-read the card now that we hold the column locks: another mover of the
        // same card may have committed between requireCard() and the locks above
        em.refresh(card);
        UUID sourceColumnId = card.getColumn().getId();
        if (!lockedColumnIds.contains(sourceColumnId)) {
            throw new ConflictException("Card was moved concurrently, retry");
        }
        int oldPos = card.getPosition();
        boolean sameColumn = sourceColumnId.equals(target.getId());

        // 1. remove from source: park the card out of the way, close the gap
        card.setPosition(-1);
        cards.saveAndFlush(card);
        cards.shiftDownAfter(sourceColumnId, oldPos);

        // 2. clamp target position
        int targetCount = cards.countByColumnId(target.getId());
        if (sameColumn) targetCount--;
        int newPos = Math.min(Math.max(req.position(), 0), targetCount);

        // 3. open gap in target and drop the card in
        cards.shiftUpFrom(target.getId(), newPos);
        card.setColumn(target);
        card.setPosition(newPos);
        return CardDtos.toResponse(cards.saveAndFlush(card));
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
