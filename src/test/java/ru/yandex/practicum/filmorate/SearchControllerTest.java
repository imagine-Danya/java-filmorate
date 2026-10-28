package ru.yandex.practicum.filmorate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@Sql(scripts = "classpath:schema.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_CLASS)
@Sql(scripts = "classpath:data.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_CLASS)
class SearchControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    private Integer directorId;
    private Integer film1Id;
    private Integer film2Id;
    private Integer user1Id;

    @BeforeEach
    void setUp() {
        jdbc.update("DELETE FROM film_directors WHERE film_id > 3");
        jdbc.update("DELETE FROM likes WHERE film_id > 3 OR user_id > 3");
        jdbc.update("DELETE FROM films WHERE film_id > 3");
        jdbc.update("DELETE FROM users WHERE user_id > 3");
        jdbc.update("DELETE FROM directors WHERE director_id > 2");

        jdbc.update("ALTER TABLE users ALTER COLUMN user_id RESTART WITH 4");
        jdbc.update("ALTER TABLE films ALTER COLUMN film_id RESTART WITH 4");
        jdbc.update("ALTER TABLE directors ALTER COLUMN director_id RESTART WITH 3");

        jdbc.update("INSERT INTO directors (name, surname) VALUES ('Кристофер', 'Нолан')");
        directorId = jdbc.queryForObject("SELECT director_id FROM directors WHERE surname = 'Нолан'", Integer.class);

        jdbc.update("INSERT INTO users (email, login, name, birthday) VALUES ('u1@mail.ru', 'u1', 'User1', '1990-01-01')");
        user1Id = jdbc.queryForObject("SELECT user_id FROM users WHERE login = 'u1'", Integer.class);

        jdbc.update("INSERT INTO films (name, description, release_date, duration, mpa_rating_id) VALUES ('Интерстеллар', 'Космос', '2014-11-07', 169, 1)");
        film1Id = jdbc.queryForObject("SELECT film_id FROM films WHERE name = 'Интерстеллар'", Integer.class);

        jdbc.update("INSERT INTO films (name, description, release_date, duration, mpa_rating_id) VALUES ('Начало', 'Сон', '2010-07-16', 148, 1)");
        film2Id = jdbc.queryForObject("SELECT film_id FROM films WHERE name = 'Начало'", Integer.class);

        jdbc.update("INSERT INTO film_directors (film_id, director_id) VALUES (?, ?)", film1Id, directorId);
        jdbc.update("INSERT INTO film_directors (film_id, director_id) VALUES (?, ?)", film2Id, directorId);
    }

    @Test
    void searchByTitle_ReturnsMatchingFilms() throws Exception {
        mockMvc.perform(get("/films/search")
                        .param("query", "интер")
                        .param("by", "title"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Интерстеллар"));
    }

    @Test
    void searchByDirector_ReturnsFilmsOfDirector() throws Exception {
        mockMvc.perform(get("/films/search")
                        .param("query", "нолан")
                        .param("by", "director"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void searchByTitleAndDirector_ReturnsUnion() throws Exception {
        mockMvc.perform(get("/films/search")
                        .param("query", "нол")
                        .param("by", "title,director"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void search_ReturnsEmptyListIfNotFound() throws Exception {
        mockMvc.perform(get("/films/search")
                        .param("query", "абракадабра")
                        .param("by", "title"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void search_SortedByPopularity() throws Exception {
        jdbc.update("INSERT INTO likes (film_id, user_id) VALUES (?, ?)", film2Id, user1Id);

        mockMvc.perform(get("/films/search")
                        .param("query", "нолан")
                        .param("by", "director"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].name").value("Начало"))
                .andExpect(jsonPath("$[1].name").value("Интерстеллар"));
    }
}