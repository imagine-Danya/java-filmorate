package ru.yandex.practicum.filmorate;

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
class FilmControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void searchFilms_ReturnsMatchingFilms() throws Exception {
        jdbc.update("INSERT INTO films (name, description, release_date, duration, mpa_rating_id) VALUES ('Интерстеллар', 'Космос', '2014-11-07', 169, 1)");

        mockMvc.perform(get("/films/search").param("query", "интер"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Интерстеллар"));
    }

    @Test
    void searchFilms_ReturnsNotFoundWhenNoMatches() throws Exception {
        jdbc.update("INSERT INTO films (name, description, release_date, duration, mpa_rating_id) VALUES ('Интерстеллар', 'Космос', '2014-11-07', 169, 1)");

        mockMvc.perform(get("/films/search").param("query", "Абракадабра"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Фильмы по запросу 'Абракадабра' не найдены"));
    }
}