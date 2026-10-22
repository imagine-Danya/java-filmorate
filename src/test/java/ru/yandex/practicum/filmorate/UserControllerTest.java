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
@Sql(scripts = "classpath:schema.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(scripts = "classpath:data.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    private Integer user1Id;
    private Integer user2Id;
    private Integer film1Id;
    private Integer film2Id;
    private Integer film3Id;

    @BeforeEach
    void setUp() {
        // Создаем пользователей
        jdbc.update("INSERT INTO users (email, login, name, birthday) VALUES ('u1@mail.ru', 'u1', 'User1', '1990-01-01')");
        jdbc.update("INSERT INTO users (email, login, name, birthday) VALUES ('u2@mail.ru', 'u2', 'User2', '1990-01-01')");
        user1Id = jdbc.queryForObject("SELECT user_id FROM users WHERE login = 'u1'", Integer.class);
        user2Id = jdbc.queryForObject("SELECT user_id FROM users WHERE login = 'u2'", Integer.class);

        // Создаем фильмы
        jdbc.update("INSERT INTO films (name, description, release_date, duration, mpa_rating_id) VALUES ('Фильм 1', 'Описание', '2010-01-01', 100, 1)");
        jdbc.update("INSERT INTO films (name, description, release_date, duration, mpa_rating_id) VALUES ('Фильм 2', 'Описание', '2011-01-01', 100, 1)");
        jdbc.update("INSERT INTO films (name, description, release_date, duration, mpa_rating_id) VALUES ('Фильм 3', 'Описание', '2012-01-01', 100, 1)");
        film1Id = jdbc.queryForObject("SELECT film_id FROM films WHERE name = 'Фильм 1'", Integer.class);
        film2Id = jdbc.queryForObject("SELECT film_id FROM films WHERE name = 'Фильм 2'", Integer.class);
        film3Id = jdbc.queryForObject("SELECT film_id FROM films WHERE name = 'Фильм 3'", Integer.class);
    }

    @Test
    void findCommonFilms_ReturnsCommonFilms() throws Exception {
        jdbc.update("INSERT INTO likes (film_id, user_id) VALUES (?, ?)", film1Id, user1Id);
        jdbc.update("INSERT INTO likes (film_id, user_id) VALUES (?, ?)", film2Id, user1Id);
        jdbc.update("INSERT INTO likes (film_id, user_id) VALUES (?, ?)", film1Id, user2Id);
        jdbc.update("INSERT INTO likes (film_id, user_id) VALUES (?, ?)", film2Id, user2Id);

        mockMvc.perform(get("/users/{id}/films/common/{otherId}", user1Id, user2Id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void findCommonFilms_ReturnsNotFoundWhenNoCommonFilms() throws Exception {
        jdbc.update("INSERT INTO likes (film_id, user_id) VALUES (?, ?)", film1Id, user1Id);
        jdbc.update("INSERT INTO likes (film_id, user_id) VALUES (?, ?)", film2Id, user2Id);

        mockMvc.perform(get("/users/{id}/films/common/{otherId}", user1Id, user2Id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Общих избранных фильмов нет"));
    }

    @Test
    void findRecommendations_ReturnsRecommendedFilms() throws Exception {
        // Юзер 1 любит 1 и 2, Юзер 2 любит 1, 2 и 3
        jdbc.update("INSERT INTO likes (film_id, user_id) VALUES (?, ?)", film1Id, user1Id);
        jdbc.update("INSERT INTO likes (film_id, user_id) VALUES (?, ?)", film2Id, user1Id);
        jdbc.update("INSERT INTO likes (film_id, user_id) VALUES (?, ?)", film1Id, user2Id);
        jdbc.update("INSERT INTO likes (film_id, user_id) VALUES (?, ?)", film2Id, user2Id);
        jdbc.update("INSERT INTO likes (film_id, user_id) VALUES (?, ?)", film3Id, user2Id);

        mockMvc.perform(get("/users/{id}/recommendations", user1Id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Фильм 3"));
    }

    @Test
    void findRecommendations_ReturnsNotFoundWhenNoRecommendations() throws Exception {
        mockMvc.perform(get("/users/{id}/recommendations", user1Id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Извините, ничего не можем порекомендовать "));
    }
}
