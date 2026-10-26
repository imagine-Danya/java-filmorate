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
class DirectorControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    private Integer directorId;
    private Integer film1Id;
    private Integer film2Id;
    private Integer user1Id;
    private Integer user2Id;

    @BeforeEach
    void setUp() {
        jdbc.update("INSERT INTO directors (name, surname) VALUES ('Кристофер', 'Нолан')");
        directorId = jdbc.queryForObject("SELECT director_id FROM directors WHERE surname = 'Нолан'", Integer.class);


        jdbc.update("INSERT INTO users (email, login, name, birthday) VALUES ('u1@mail.ru', 'u1', 'User1', '1990-01-01')");
        jdbc.update("INSERT INTO users (email, login, name, birthday) VALUES ('u2@mail.ru', 'u2', 'User2', '1990-01-01')");
        user1Id = jdbc.queryForObject("SELECT user_id FROM users WHERE login = 'u1'", Integer.class);
        user2Id = jdbc.queryForObject("SELECT user_id FROM users WHERE login = 'u2'", Integer.class);


        jdbc.update("INSERT INTO films (name, description, release_date, duration, mpa_rating_id) VALUES ('Начало', 'Сон', '2010-07-16', 148, 1)");
        film1Id = jdbc.queryForObject("SELECT film_id FROM films WHERE name = 'Начало'", Integer.class);

        jdbc.update("INSERT INTO films (name, description, release_date, duration, mpa_rating_id) VALUES ('Интерстеллар', 'Космос', '2014-11-07', 169, 1)");
        film2Id = jdbc.queryForObject("SELECT film_id FROM films WHERE name = 'Интерстеллар'", Integer.class);

        jdbc.update("INSERT INTO film_directors (film_id, director_id) VALUES (?, ?)", film1Id, directorId);
        jdbc.update("INSERT INTO film_directors (film_id, director_id) VALUES (?, ?)", film2Id, directorId);
    }

    @Test
    void testGetFilmsByDirector_SortByYear() throws Exception {
        mockMvc.perform(get("/films/director/{directorId}", directorId)
                        .param("sortBy", "year"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].name").value("Начало"))
                .andExpect(jsonPath("$[1].name").value("Интерстеллар"));
    }

    @Test
    void testGetFilmsByDirector_SortByLikes() throws Exception {
        jdbc.update("INSERT INTO likes (film_id, user_id) VALUES (?, ?)", film2Id, user1Id);
        jdbc.update("INSERT INTO likes (film_id, user_id) VALUES (?, ?)", film2Id, user2Id);

        mockMvc.perform(get("/films/director/{directorId}", directorId)
                        .param("sortBy", "likes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].name").value("Интерстеллар"))
                .andExpect(jsonPath("$[1].name").value("Начало"));
    }

    @Test
    void testGetFilmsByDirector_DirectorNotFound() throws Exception {
        mockMvc.perform(get("/films/director/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Режиссёр с id 999 не найден"));
    }

    @Test
    void testGetFilmsByDirector_InvalidSortParam() throws Exception {
        mockMvc.perform(get("/films/director/{directorId}", directorId)
                        .param("sortBy", "invalid_sort"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Параметр sortBy должен быть 'year' или 'likes'"));
    }
}