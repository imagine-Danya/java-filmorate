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
class RecommendationsTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    private Integer user1Id;
    private Integer user2Id;
    private Integer user3Id;

    private Integer film1Id;
    private Integer film2Id;
    private Integer film3Id;
    private Integer film4Id;

    @BeforeEach
    void setUp() {
        ensureReferenceDataExists();

        jdbc.update("INSERT INTO users (email, login, name, birthday) VALUES ('u1@mail.ru', 'u1', 'User1', '1990-01-01')");
        jdbc.update("INSERT INTO users (email, login, name, birthday) VALUES ('u2@mail.ru', 'u2', 'User2', '1990-01-01')");
        jdbc.update("INSERT INTO users (email, login, name, birthday) VALUES ('u3@mail.ru', 'u3', 'User3', '1990-01-01')");

        user1Id = jdbc.queryForObject("SELECT user_id FROM users WHERE login = 'u1'", Integer.class);
        user2Id = jdbc.queryForObject("SELECT user_id FROM users WHERE login = 'u2'", Integer.class);
        user3Id = jdbc.queryForObject("SELECT user_id FROM users WHERE login = 'u3'", Integer.class);

        jdbc.update("INSERT INTO films (name, description, release_date, duration, mpa_rating_id) VALUES ('Фильм 1', 'Описание', '2020-01-01', 120, 1)");
        film1Id = jdbc.queryForObject("SELECT film_id FROM films WHERE name = 'Фильм 1'", Integer.class);

        jdbc.update("INSERT INTO films (name, description, release_date, duration, mpa_rating_id) VALUES ('Фильм 2', 'Описание', '2020-01-01', 120, 1)");
        film2Id = jdbc.queryForObject("SELECT film_id FROM films WHERE name = 'Фильм 2'", Integer.class);

        jdbc.update("INSERT INTO films (name, description, release_date, duration, mpa_rating_id) VALUES ('Фильм 3', 'Описание', '2020-01-01', 120, 1)");
        film3Id = jdbc.queryForObject("SELECT film_id FROM films WHERE name = 'Фильм 3'", Integer.class);

        jdbc.update("INSERT INTO films (name, description, release_date, duration, mpa_rating_id) VALUES ('Фильм 4', 'Описание', '2020-01-01', 120, 1)");
        film4Id = jdbc.queryForObject("SELECT film_id FROM films WHERE name = 'Фильм 4'", Integer.class);

        jdbc.update("INSERT INTO likes (film_id, user_id) VALUES (?, ?)", film1Id, user1Id);
        jdbc.update("INSERT INTO likes (film_id, user_id) VALUES (?, ?)", film2Id, user1Id);
        jdbc.update("INSERT INTO likes (film_id, user_id) VALUES (?, ?)", film1Id, user2Id);
        jdbc.update("INSERT INTO likes (film_id, user_id) VALUES (?, ?)", film2Id, user2Id);
        jdbc.update("INSERT INTO likes (film_id, user_id) VALUES (?, ?)", film3Id, user2Id);
        jdbc.update("INSERT INTO likes (film_id, user_id) VALUES (?, ?)", film4Id, user3Id);
    }

    private void ensureReferenceDataExists() {
        Integer mpaCount = jdbc.queryForObject("SELECT COUNT(*) FROM mpa_ratings", Integer.class);
        if (mpaCount == null || mpaCount == 0) {
            jdbc.update("MERGE INTO mpa_ratings (mpa_rating_id, name, description) KEY (mpa_rating_id) VALUES " +
                    "(1, 'G', 'У фильма нет возрастных ограничений')," +
                    "(2, 'PG', 'Детям рекомендуется смотреть фильм с родителями')," +
                    "(3, 'PG-13', 'Детям до 13 лет просмотр не желателен')," +
                    "(4, 'R', 'Лицам до 17 лет просматривать фильм только в сопровождении одного из родителей')," +
                    "(5, 'NC-17', 'Лицам до 18 лет просмотр запрещён')");
        }

        Integer genreCount = jdbc.queryForObject("SELECT COUNT(*) FROM genres", Integer.class);
        if (genreCount == null || genreCount == 0) {
            jdbc.update("MERGE INTO genres (genre_id, name) KEY (genre_id) VALUES " +
                    "(1, 'Комедия')," +
                    "(2, 'Драма')," +
                    "(3, 'Мультфильм')," +
                    "(4, 'Триллер')," +
                    "(5, 'Документальный')," +
                    "(6, 'Боевик')");
        }
    }

    @Test
    void getRecommendations_ReturnsFilmsBasedOnSimilarUsers() throws Exception {
        mockMvc.perform(get("/users/{id}/recommendations", user1Id))
                .andDo(result -> {
                    if (result.getResponse().getStatus() == 500) {
                        System.err.println("Ошибка 500: " + result.getResponse().getContentAsString());
                    }
                })
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Фильм 3"));
    }

    @Test
    void getRecommendations_ReturnsEmptyListIfNoSimilarUsers() throws Exception {
        mockMvc.perform(get("/users/{id}/recommendations", user3Id))
                .andDo(result -> {
                    if (result.getResponse().getStatus() == 500) {
                        System.err.println("Ошибка 500: " + result.getResponse().getContentAsString());
                    }
                })
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }
}