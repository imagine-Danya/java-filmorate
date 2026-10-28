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
class CommonFilmsTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    private Integer user1Id;
    private Integer user2Id;
    private Integer film1Id;
    private Integer film2Id;

    @BeforeEach
    void setUp() {
        jdbc.update("DELETE FROM likes WHERE user_id > 3 OR film_id > 3");
        jdbc.update("DELETE FROM films WHERE film_id > 3");
        jdbc.update("DELETE FROM users WHERE user_id > 3");
        jdbc.update("ALTER TABLE users ALTER COLUMN user_id RESTART WITH 4");
        jdbc.update("ALTER TABLE films ALTER COLUMN film_id RESTART WITH 4");
        jdbc.update("INSERT INTO users (email, login, name, birthday) VALUES ('u1@mail.ru', 'u1', 'User1', '1990-01-01')");
        jdbc.update("INSERT INTO users (email, login, name, birthday) VALUES ('u2@mail.ru', 'u2', 'User2', '1990-01-01')");
        user1Id = jdbc.queryForObject("SELECT user_id FROM users WHERE login = 'u1'", Integer.class);
        user2Id = jdbc.queryForObject("SELECT user_id FROM users WHERE login = 'u2'", Integer.class);
        jdbc.update("INSERT INTO films (name, description, release_date, duration, mpa_rating_id) VALUES ('Общий фильм', 'Описание', '2020-01-01', 120, 1)");
        film1Id = jdbc.queryForObject("SELECT film_id FROM films WHERE name = 'Общий фильм'", Integer.class);
        jdbc.update("INSERT INTO films (name, description, release_date, duration, mpa_rating_id) VALUES ('Личный фильм', 'Описание', '2021-01-01', 90, 1)");
        film2Id = jdbc.queryForObject("SELECT film_id FROM films WHERE name = 'Личный фильм'", Integer.class);
        jdbc.update("INSERT INTO likes (film_id, user_id) VALUES (?, ?)", film1Id, user1Id);
        jdbc.update("INSERT INTO likes (film_id, user_id) VALUES (?, ?)", film1Id, user2Id);
        jdbc.update("INSERT INTO likes (film_id, user_id) VALUES (?, ?)", film2Id, user1Id);
    }

    @Test
    void getCommonFilms_ReturnsOnlyCommonFilms() throws Exception {
        mockMvc.perform(get("/films/common")
                        .param("userId", user1Id.toString())
                        .param("friendId", user2Id.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Общий фильм"));
    }

    @Test
    void getCommonFilms_ReturnsEmptyListIfNoCommonFilms() throws Exception {
        jdbc.update("INSERT INTO users (email, login, name, birthday) VALUES ('u3@mail.ru', 'u3', 'User3', '1990-01-01')");
        Integer user3Id = jdbc.queryForObject("SELECT user_id FROM users WHERE login = 'u3'", Integer.class);

        mockMvc.perform(get("/films/common")
                        .param("userId", user1Id.toString())
                        .param("friendId", user3Id.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }
}