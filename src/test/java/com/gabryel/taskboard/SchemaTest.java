package com.gabryel.taskboard;

import com.gabryel.taskboard.auth.User;
import com.gabryel.taskboard.auth.UserRepository;
import com.gabryel.taskboard.card.Card;
import com.gabryel.taskboard.card.CardRepository;
import com.gabryel.taskboard.card.Priority;
import com.gabryel.taskboard.column.BoardColumn;
import com.gabryel.taskboard.column.BoardColumnRepository;
import com.gabryel.taskboard.project.Project;
import com.gabryel.taskboard.project.ProjectRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

class SchemaTest extends IntegrationTest {

    @Autowired UserRepository users;
    @Autowired ProjectRepository projects;
    @Autowired BoardColumnRepository columns;
    @Autowired CardRepository cards;

    @Test
    void persistsFullObjectGraph() {
        User u = new User();
        u.setEmail("schema@test.com");
        u.setPasswordHash("x");
        u.setName("Schema");
        users.save(u);

        Project p = new Project();
        p.setName("Board");
        p.setOwner(u);
        projects.save(p);

        BoardColumn col = new BoardColumn();
        col.setProject(p);
        col.setName("Todo");
        col.setPosition(0);
        columns.save(col);

        Card c = new Card();
        c.setColumn(col);
        c.setTitle("First card");
        c.setPriority(Priority.MEDIUM);
        c.setPosition(0);
        c.setCreatedBy(u);
        cards.save(c);

        assertThat(cards.findByColumnIdOrderByPosition(col.getId()))
                .hasSize(1)
                .first()
                .satisfies(saved -> assertThat(saved.getTitle()).isEqualTo("First card"));
    }
}
